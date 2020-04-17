package io.inprice.scrapper.api.app.user_company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class UserCompanyRepository {

   private static final Logger log = LoggerFactory.getLogger(UserCompanyRepository.class);
   private static final String COMPANY_SELECT_STANDARD_QUERY = "select m.*, c.name as company_name from user_company as m inner join company as c on c.id = m.company_id";

   private final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findById(long invitationId) {
      UserCompany model = db.findSingle(String.format(COMPANY_SELECT_STANDARD_QUERY + " where m.id=%d", invitationId),
            this::map);
      if (model != null) {
         if (model.getActive()) {
            return new ServiceResponse(model);
         } else {
            return Responses.NotActive.INVITATION;
         }
      } else {
         return Responses.NotFound.INVITATION;
      }
   }

   public ServiceResponse getListByUser() {
      List<UserCompany> companies = db.findMultiple(String.format(
            COMPANY_SELECT_STANDARD_QUERY + " where m.active = true and m.email = '%s' order by m.role, c.name, m.created_at desc",
            SqlHelper.clear(CurrentUser.getEmail())), this::map);
      return new ServiceResponse(companies);
   }

   public ServiceResponse getListByCompany() {
      List<UserCompany> companies = db.findMultiple(String.format(
            COMPANY_SELECT_STANDARD_QUERY + " where m.active = true and m.company_id = %d order by m.role, c.name, m.created_at desc",
            CurrentUser.getCompanyId()), this::map);
      return new ServiceResponse(companies);
   }

   public ServiceResponse getUserCompanies(String email) {
      List<UserCompany> companies = db.findMultiple(String.format(
            COMPANY_SELECT_STANDARD_QUERY + " where m.active = true and m.email = '%s' and m.status = '%s' order by m.role, m.company_id",
            SqlHelper.clear(email), UserStatus.JOINED), this::map);
      if (companies != null && companies.size() > 0) {
         return new ServiceResponse(companies);
      }
      return Responses.NotFound.INVITATION;
   }

   public UserCompany getById(long invitationId) {
      return db.findSingle(String.format("select * from user_company where id=%d and company_id=%d", invitationId, CurrentUser.getCompanyId()), this::map);
   }

   public ServiceResponse findByEmailAndCompanyId(String email, long companyId) {
      UserCompany model = db
            .findSingle(String.format(COMPANY_SELECT_STANDARD_QUERY + " where m.email='%s' and m.company_id=%d",
                  SqlHelper.clear(email), companyId), this::map);
      if (model != null) {
         if (model.getActive()) {
            return new ServiceResponse(model);
         } else {
            return Responses.NotActive.INVITATION;
         }
      } else {
         return Responses.NotFound.INVITATION;
      }
   }

   public ServiceResponse deleteById(Long id) {
      boolean result = 
         db.executeQuery(
            String.format("delete from user_company where id=%d and company_id=%d", id, CurrentUser.getCompanyId()),
            "Failed to delete member! id: " + id);

      if (result)
         return Responses.OK;
      else
         return Responses.NotFound.PRODUCT;
   }

   public ServiceResponse toggleStatus(Long invitationId) {
      boolean result = db
         .executeQuery(String.format("update user_company set active = not active where id = %d and company_id = %d ", invitationId,
            CurrentUser.getCompanyId()), "Failed to toggle product status! id: " + invitationId);

      if (result) {
         return Responses.OK;
      }
      return Responses.NotFound.INVITATION;
   }

   public ServiceResponse changeRole(MemberChangeRoleDTO dto) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      try (Connection con = db.getConnection();
            PreparedStatement pstUpdate = con
                  .prepareStatement("update user_company set role=? where id=? and company_id=?")) {
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

   private UserCompany map(ResultSet rs) {
      try {
         UserCompany model = new UserCompany();
         model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
         model.setActive(rs.getBoolean("active"));
         model.setUserId(RepositoryHelper.nullLongHandler(rs, "user_id"));
         model.setEmail(rs.getString("email"));
         model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
         model.setCompanyName(rs.getString("company_name")); // transient
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
