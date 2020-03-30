package io.inprice.scrapper.api.app.member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;

public class MemberRepository {

   private static final Logger log = LoggerFactory.getLogger(MemberRepository.class);
   private static final String COMPANY_SELECT_STANDARD_QUERY = "select m.*, c.name as company_name from member as m inner join company as c on c.id = m.company_id";

   private final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findById(long memberId) {
      Member model = db.findSingle(String.format(COMPANY_SELECT_STANDARD_QUERY + " where m.id=%d", memberId),
            this::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.MEMBER;
   }

   public ServiceResponse findByEmailAndCompanyId(String email, long companyId) {
      Member model = db
            .findSingle(String.format(COMPANY_SELECT_STANDARD_QUERY + " where m.email='%s' and m.company_id=%d",
                  SqlHelper.clear(email), companyId), this::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.MEMBER;
   }

   public ServiceResponse getListByUser() {
      List<Member> members = db.findMultiple(String.format(
            COMPANY_SELECT_STANDARD_QUERY + " where m.email = '%s' order by m.role, c.name, m.created_at desc",
            SqlHelper.clear(CurrentUser.getEmail())), this::map);
      return new ServiceResponse(members);
   }

   public ServiceResponse getListByCompany() {
      List<Member> members = db.findMultiple(String.format(
            COMPANY_SELECT_STANDARD_QUERY + " where m.company_id = %d order by m.role, c.name, m.created_at desc",
            CurrentUser.getCompanyId()), this::map);
      return new ServiceResponse(members);
   }

   public ServiceResponse findASuitableCompanyId(String email) {
      List<Member> members = db.findMultiple(String.format(
            COMPANY_SELECT_STANDARD_QUERY + " where m.email = '%s' and m.status = '%s' order by m.role, m.company_id",
            SqlHelper.clear(email), MemberStatus.JOINED), this::map);

      if (members != null && members.size() > 0) {
         return new ServiceResponse(members.get(0));
      }
      return Responses.NotFound.COMPANY;
   }

   public ServiceResponse insert(MemberDTO memberDTO) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // company is inserted
      try (Connection con = db.getConnection();
            PreparedStatement pst = con
                  .prepareStatement("insert into member (email, role, company_id) values (?, ?, ?) ")) {
         int i = 0;
         pst.setString(++i, memberDTO.getEmail());
         pst.setString(++i, memberDTO.getRole().name());
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            response = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to insert a new member. " + memberDTO, e);
      }

      return response;
   }

   public ServiceResponse toggleStatus(Long memberId) {
      boolean result = db
         .executeQuery(String.format("update member set active = not active where id = %d and company_id = %d ", memberId,
            CurrentUser.getCompanyId()), "Failed to toggle product status! id: " + memberId);

      if (result) {
         return Responses.OK;
      }
      return Responses.NotFound.MEMBER;
   }

   public ServiceResponse changeRole(MemberChangeRoleDTO dto) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      try (Connection con = db.getConnection();
            PreparedStatement pstUpdate = con
                  .prepareStatement("update member set role=? where id=? and company_id=?")) {
         int i = 0;
         pstUpdate.setString(++i, dto.getRole().name());
         pstUpdate.setLong(++i, dto.getMemberId());
         pstUpdate.setLong(++i, CurrentUser.getCompanyId());

         if (pstUpdate.executeUpdate() > 0) {
            response = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to change role of a member. " + dto, e);
      }

      return response;
   }

   public ServiceResponse increaseSendingCount(long memberId) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // company is inserted
      try (Connection con = db.getConnection();
            PreparedStatement pst = con.prepareStatement(
                  "update member set retry=retry+1 where id=? and retry<3 and status=? and company_id=?")) {
         int i = 0;
         pst.setLong(++i, memberId);
         pst.setString(++i, MemberStatus.PENDING.name());
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            response = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to increase retry count. Member Id: " + memberId, e);
      }

      return response;
   }

   private Member map(ResultSet rs) {
      try {
         Member model = new Member();
         model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
         model.setActive(rs.getBoolean("active"));
         model.setEmail(rs.getString("email"));
         model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
         model.setCompanyName(rs.getString("company_name")); // transient
         model.setRole(MemberRole.valueOf(rs.getString("role")));
         model.setStatus(MemberStatus.valueOf(rs.getString("status")));
         model.setRetry(rs.getInt("retry"));
         model.setCreatedAt(rs.getDate("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set member's properties", e);
      }
      return null;
   }

}
