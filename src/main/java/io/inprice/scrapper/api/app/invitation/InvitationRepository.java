package io.inprice.scrapper.api.app.invitation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.member.Member;
import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.app.member.MemberStatus;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.InvitationDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class InvitationRepository {

   private static final Logger log = LoggerFactory.getLogger(InvitationRepository.class);

   private final Database db = Beans.getSingleton(Database.class);
   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

   public ServiceResponse findById(long memberId) {
      Member model = db.findSingle(String.format("select * from member where id=%d", memberId), this::map);

      if (model != null) {
         return new ServiceResponse(model);
      } else {
         return Responses.NotFound.MEMBER;
      }
   }

   public ServiceResponse findByEmailAndCompanyId(String email, long companyId) {
      Member model = db
            .findSingle(String.format("select * from member where email='%s' and company_id=%d",
                  SqlHelper.clear(email), companyId), this::map);
      if (model != null) {
         return new ServiceResponse(model);
      } else {
         return Responses.NotFound.MEMBER;
      }
   }

   public ServiceResponse invite(InvitationDTO dto) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // member is inserted
      try (Connection con = db.getConnection();
            PreparedStatement pst = con
                  .prepareStatement("insert into member (email, role, company_id) values (?, ?, ?) ")) {
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

   public ServiceResponse acceptNewUser(InvitationAcceptDTO acceptDTO, InvitationDTO invitationDTO) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      Connection con = null;

      try {
         con = db.getTransactionalConnection();

         Member member = 
            db.findSingle(
               con,
               String.format("select * from member where email='%s' and company_id=%d",
               invitationDTO.getEmail(), invitationDTO.getCompanyId()), this::map);

         res = checkMembership(member);
         if (res.isOK()) {

            try (PreparedStatement pst = con.prepareStatement("update member set status=? where email=? and company_id=?")) {
               int i = 0;
               pst.setString(++i, MemberStatus.JOINED.name());
               pst.setString(++i, invitationDTO.getEmail());
               pst.setLong(++i, invitationDTO.getCompanyId());
      
               if (pst.executeUpdate() > 0) {

                  res = userRepository.findByEmail(con, invitationDTO.getEmail());
                  if (Responses.NotFound.USER.equals(res)) {
                     UserDTO dto = new UserDTO();
                     dto.setName(acceptDTO.getName());
                     dto.setEmail(invitationDTO.getEmail());
                     dto.setPassword(acceptDTO.getPassword());
                     dto.setCompanyId(invitationDTO.getCompanyId());
                     res = userRepository.insert(con, dto);
                  } else {
                     res = Responses.OK;
                  }

                  if (Responses.OK.equals(res)) {
                     db.commit(con);
                  } else {
                     db.rollback(con);
                  }
               }
            }
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

   public ServiceResponse handleExisting(Long memberId, boolean isAcceptance) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      try (Connection con = db.getConnection()) {

         Member member = 
            db.findSingle(
               con,
               String.format("select * from member where id=%d and email='%s'", memberId, CurrentUser.getEmail()), this::map);

         res = checkMembership(member);
         if (res.isOK()) {

            try (PreparedStatement pst = con.prepareStatement("update member set status=? where id=?")) {
               int i = 0;
               if (isAcceptance) {
                  pst.setString(++i, MemberStatus.JOINED.name());
               } else {
                  pst.setString(++i, MemberStatus.LEFT.name());
               }
               pst.setLong(++i, memberId);
      
               if (pst.executeUpdate() > 0) {
                  res = Responses.OK;
               }
            }
         }

      } catch (SQLException e) {
         log.error("Failed to " + (isAcceptance ? " accept " : " reject ") + " an existing member. Member Id: " + memberId, e);
      }

      return res;
   }

   public ServiceResponse increaseSendingCount(long memberId) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // company is inserted
      try (Connection con = db.getConnection();
            PreparedStatement pst = con.prepareStatement(
                  "update member set retry = retry + 1 where active = true and id = ? and retry < 3 and status = ? and company_id = ?")) {
         int i = 0;
         pst.setLong(++i, memberId);
         pst.setString(++i, MemberStatus.PENDING.name());
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            res = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to increase retry count. Member Id: " + memberId, e);
      }

      return res;
   }

   private ServiceResponse checkMembership(Member member) {
      ServiceResponse res = Responses.OK;
      if (member != null) {
         if (member.getActive() && MemberStatus.PENDING.equals(member.getStatus())) {
            res = Responses.OK;
         } else {
            res = Responses.NotActive.MEMBER;
         }
      } else {
         res = Responses.NotFound.MEMBER;
      }
      return res;
   }

   private Member map(ResultSet rs) {
      try {
         Member model = new Member();
         model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
         model.setActive(rs.getBoolean("active"));
         model.setEmail(rs.getString("email"));
         model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
         model.setRole(MemberRole.valueOf(rs.getString("role")));
         model.setStatus(MemberStatus.valueOf(rs.getString("status")));
         model.setRetry(rs.getInt("retry"));
         model.setCreatedAt(rs.getTimestamp("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set member's properties", e);
      }
      return null;
   }

}
