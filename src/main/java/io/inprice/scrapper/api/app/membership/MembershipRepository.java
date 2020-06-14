package io.inprice.scrapper.api.app.membership;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.InvitationSendDTO;
import io.inprice.scrapper.api.dto.InvitationUpdateDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.common.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.UserRole;
import io.inprice.scrapper.common.meta.UserStatus;
import io.inprice.scrapper.common.models.Membership;
import io.inprice.scrapper.common.models.User;

public class MembershipRepository {

  private static final Logger log = LoggerFactory.getLogger(MembershipRepository.class);

  private final Database db = Beans.getSingleton(Database.class);
  private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

  public ServiceResponse getList() {
    List<Membership> memberList = 
      db.findMultiple(
        String.format(
          "select m.*, c.currency_format, c.name as company_name, c.plan_id, p.name as plan_name from membership as m " +
          "inner join company as c on c.id = m.company_id " + 
          "left join plan as p on p.id = c.plan_id " + 
          "where m.email != '%s' " + 
          "  and company_id = %d " + 
          "order by m.email",
        CurrentUser.getEmail(), CurrentUser.getCompanyId()), this::mapWithCompany);

    if (memberList != null && memberList.size() > 0) {
      return new ServiceResponse(memberList);
    }
    return Responses.NotFound.MEMBERSHIP;
  }

  public ServiceResponse findById(long memberId) {
    Membership model = db.findSingle(String.format("select * from membership where id=%d", memberId), this::map);

    if (model != null) {
      return new ServiceResponse(model);
    } else {
      return Responses.NotFound.MEMBERSHIP;
    }
  }

  public ServiceResponse findByEmailAndCompanyId(String email, long companyId) {
    Membership model = db.findSingle(
        String.format("select * from membership where email='%s' and company_id=%d", SqlHelper.clear(email), companyId),
        this::map);
    if (model != null) {
      return new ServiceResponse(model);
    } else {
      return Responses.NotFound.MEMBERSHIP;
    }
  }

  public ServiceResponse invite(InvitationSendDTO dto) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    try (Connection con = db.getConnection();
        PreparedStatement pst = con
            .prepareStatement("insert into membership (email, role, company_id) values (?, ?, ?) ")) {
      int i = 0;
      pst.setString(++i, dto.getEmail());
      pst.setString(++i, dto.getRole().name());
      pst.setLong(++i, CurrentUser.getCompanyId());

      if (pst.executeUpdate() > 0) {
        res = Responses.OK;
      }
    } catch (SQLException e) {
      log.error("Failed to insert a new member. " + dto, e);
    }

    return res;
  }

  public ServiceResponse changeRole(InvitationUpdateDTO dto) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    try (Connection con = db.getConnection();
        PreparedStatement pst = con.prepareStatement("update membership set role=? where id=? and company_id=?")) {
      int i = 0;
      pst.setString(++i, dto.getRole().name());
      pst.setLong(++i, dto.getId());
      pst.setLong(++i, CurrentUser.getCompanyId());

      if (pst.executeUpdate() > 0) {
        res = Responses.OK;
      }
    } catch (SQLException e) {
      log.error("Failed to change role of member. " + dto, e);
    }

    return res;
  }

  public ServiceResponse changeStatus(Long id, boolean isResumed) {
    String part1 = "pre_status=status, status='PAUSED'";
    String part2 = "and status!='PAUSED'";
    if (isResumed) {
      part1 = "status=pre_status, pre_status='PAUSED'";
      part2 = "and status='PAUSED'";
    }

    try (Connection con = db.getConnection();
        PreparedStatement pst = 
          con.prepareStatement(
            "update membership set " + part1 + ", updated_at=now() where id=? and company_id=? " + part2)) {
      int i = 0;
      pst.setLong(++i, id);
      pst.setLong(++i, CurrentUser.getCompanyId());

      if (pst.executeUpdate() > 0) {
        return Responses.OK;
      } else {
        return Responses.DataProblem.NOT_SUITABLE;
      }
    } catch (SQLException e) {
      log.error("Failed to update status member. UserId: " + id + ", isResumed: " + isResumed , e);
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  public ServiceResponse acceptNewUser(InvitationAcceptDTO acceptDTO, String timezone, InvitationSendDTO invitationDTO) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;

    try {
      con = db.getTransactionalConnection();

      Membership membership = db.findSingle(con,
          String.format("select * from membership where email='%s' and company_id=%d and status='%s'",
              invitationDTO.getEmail(), invitationDTO.getCompanyId(), UserStatus.PENDING),
          this::map);

      res = checkMembership(membership);
      if (res.isOK()) {

        res = userRepository.findByEmail(con, invitationDTO.getEmail());
        if (Responses.NotFound.USER.equals(res)) {
          UserDTO dto = new UserDTO();
          dto.setName(invitationDTO.getEmail().split("@")[0]);
          dto.setEmail(invitationDTO.getEmail());
          dto.setTimezone(timezone);
          dto.setPassword(acceptDTO.getPassword());
          dto.setCompanyId(invitationDTO.getCompanyId());
          res = userRepository.insert(con, dto);
        } else {
          res = Responses.Already.Defined.MEMBERSHIP;
        }

        if (res.isOK()) {
          User user = res.getData();

          try (PreparedStatement pst = con.prepareStatement(
              "update membership set user_id=?, status=?, updated_at=now() " + 
              "where email=? and company_id=? and status=?")) {
            int i = 0;
            pst.setLong(++i, user.getId());
            pst.setString(++i, UserStatus.JOINED.name());
            pst.setString(++i, invitationDTO.getEmail());
            pst.setLong(++i, invitationDTO.getCompanyId());
            pst.setString(++i, UserStatus.PENDING.name());

            if (pst.executeUpdate() > 0) {

              if (res.isOK()) {
                db.commit(con);
              } else {
                db.rollback(con);
              }
            } else {
              res = Responses.NotFound.MEMBERSHIP;
            }
          }
        }
      } else {
        res = Responses.NotFound.INVITATION;
      }

    } catch (SQLException e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to accept a new member. " + invitationDTO, e);
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

  public ServiceResponse acceptExisting(Long invitationId) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    try (Connection con = db.getConnection()) {

      Membership membership = db.findSingle(con,
          String.format("select * from membership where id=%d and email='%s'", invitationId, CurrentUser.getEmail()),
          this::map);

      res = checkMembership(membership);
      if (res.isOK()) {

        try (PreparedStatement pst = con.prepareStatement("update membership set status=? where id=?")) {
          int i = 0;
          pst.setString(++i, UserStatus.JOINED.name());
          pst.setLong(++i, invitationId);

          if (pst.executeUpdate() > 0) {
            res = Responses.OK;
          }
        }
      }

    } catch (SQLException e) {
      log.error("Failed to accept an existing member. Membership Id: " + invitationId, e);
    }

    return res;
  }

  public ServiceResponse increaseSendingCount(long memberId) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    // company is inserted
    try (Connection con = db.getConnection();
        PreparedStatement pst = con.prepareStatement(
            "update membership set retry=retry+1 where id=? and retry<3 and status=? and company_id=?")) {
      int i = 0;
      pst.setLong(++i, memberId);
      pst.setString(++i, UserStatus.PENDING.name());
      pst.setLong(++i, CurrentUser.getCompanyId());

      if (pst.executeUpdate() > 0) {
        res = Responses.OK;
      } else {
        res = Responses.DataProblem.NOT_SUITABLE;
      }
    } catch (SQLException e) {
      log.error("Failed to increase retry count. Membership Id: " + memberId, e);
    }

    return res;
  }

  public ServiceResponse getUserCompanies(String email) {
    List<Membership> memberships = 
      db.findMultiple(
        String.format(
          "select m.*, c.currency_format, c.name as company_name, c.plan_id from membership as m " +
          "inner join company as c on c.id = m.company_id " + 
          "left join plan as p on p.id = c.plan_id " + 
          "where m.email='%s' " + 
          "  and m.status = '%s' " + 
          "order by m.role, m.created_at",
        SqlHelper.clear(email), UserStatus.JOINED), this::mapWithCompany);
    if (memberships != null && memberships.size() > 0) {
      return new ServiceResponse(memberships);
    }
    return Responses.NotFound.MEMBERSHIP;
  }

  public ServiceResponse delete(long memberId) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    try (Connection con = db.getConnection();
        PreparedStatement pst = con.prepareStatement(
            "update membership set status=?, updated_at=now() where id=? and status!=? and company_id=?")) {
      int i = 0;
      pst.setString(++i, UserStatus.DELETED.name());
      pst.setLong(++i, memberId);
      pst.setString(++i, UserStatus.DELETED.name());
      pst.setLong(++i, CurrentUser.getCompanyId());

      if (pst.executeUpdate() > 0) {
        res = Responses.OK;
      } else {
        res = Responses.DataProblem.NOT_SUITABLE;
      }
    } catch (SQLException e) {
      log.error("Failed to delete a user. Id: " + memberId, e);
    }

    return res;
  }

  private ServiceResponse checkMembership(Membership membership) {
    ServiceResponse res = Responses.OK;
    if (membership != null) {
      if (UserStatus.PENDING.equals(membership.getStatus())) {
        res = Responses.OK;
      } else {
        res = Responses.NotActive.INVITATION;
      }
    } else {
      res = Responses.NotFound.MEMBERSHIP;
    }
    return res;
  }

  private Membership mapWithCompany(ResultSet rs) {
    try {
      Membership model = map(rs);
      model.setUserId(RepositoryHelper.nullLongHandler(rs, "user_id"));
      model.setCompanyName(rs.getString("company_name"));
      model.setPlanId(RepositoryHelper.nullLongHandler(rs, "plan_id"));
      model.setCurrencyFormat(rs.getString("currency_format"));
      model.setPreStatus(UserStatus.valueOf(rs.getString("pre_status")));
      model.setUpdatedAt(rs.getTimestamp("updated_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set member's properties", e);
    }
    return null;
  }

  private Membership map(ResultSet rs) {
    try {
      Membership model = new Membership();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setEmail(rs.getString("email"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setRole(UserRole.valueOf(rs.getString("role")));
      model.setStatus(UserStatus.valueOf(rs.getString("status")));
      model.setRetry(rs.getInt("retry"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set member's properties", e);
    }
    return null;
  }

}
