package io.inprice.scrapper.api.app.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.utils.CodeGenerator;
import io.inprice.scrapper.api.utils.SqlHelper;
import jodd.util.BCrypt;

public class UserRepository {

   private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

   private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
   private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

   public ServiceResponse findById(Long id) {
      return findById(id, false);
   }

   public ServiceResponse findById(Long id, boolean passwordFields) {
      User model = dbUtils.findSingle(String.format("select * from user where id = %d and company_id = %d",
            id, UserInfo.getCompanyId()), this::map);
      if (model != null) {
         if (!passwordFields) {
            model.setPasswordSalt(null);
            model.setPasswordHash(null);
         }
         return new ServiceResponse(model);
      }
      return Responses.NotFound.USER;
   }

   public ServiceResponse findByEmail(String email) {
      return findByEmail(email, false);
   }

   public ServiceResponse findByEmail(String email, boolean passwordFields) {
      User model = dbUtils.findSingle(String.format("select * from user where email = '%s'", email), this::map);
      if (model != null) {
         if (!passwordFields) {
            model.setPasswordSalt(null);
            model.setPasswordHash(null);
         }
         return new ServiceResponse(model);
      }
      return Responses.NotFound.USER;
   }

   public ServiceResponse insert(UserDTO dto) {
      final String query = "insert into user "
            + "(email, name, last_company_id, password_salt, password_hash) values "
            + "(?, ?, ?, ?, ?) ";

      try (Connection con = dbUtils.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
         final String salt = codeGenerator.generateSalt();
         
         int i = 0;
         pst.setString(++i, dto.getEmail());
         pst.setString(++i, dto.getName());
         pst.setLong(++i, dto.getCompanyId());
         pst.setString(++i, salt);
         pst.setString(++i, BCrypt.hashpw(dto.getPassword(), salt));

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.DataProblem.DB_PROBLEM;

      } catch (SQLIntegrityConstraintViolationException ie) {
         log.error("Failed to insert user: " + ie.getMessage());
         return Responses.DataProblem.INTEGRITY_PROBLEM;
      } catch (Exception e) {
         log.error("Failed to insert user", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse update(UserDTO dto, boolean byAdmin, boolean passwordWillBeUpdate) {
      final String query = "update user " + "set full_name=? "
            + (!StringUtils.isBlank(dto.getEmail()) ? ", email=? " : "") + (byAdmin ? ", role=?" : "")
            + (passwordWillBeUpdate ? ", password_salt=?, password_hash=?" : "") + " where id=?"
            + "   and company_id=?";

      try (Connection con = dbUtils.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

         int i = 0;
         pst.setString(++i, dto.getFullName());

         if (!StringUtils.isBlank(dto.getEmail())) {
            pst.setString(++i, dto.getEmail());
         }

         if (byAdmin) {
            Role ut = dto.getRole();
            if (ut == null || Role.ADMIN.equals(ut))
               ut = Role.EDITOR;
            pst.setString(++i, ut.name());
         }

         if (passwordWillBeUpdate) {
            final String salt = codeGenerator.generateSalt();
            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(dto.getPassword(), salt));
         }

         pst.setLong(++i, dto.getId());
         pst.setLong(++i, UserInfo.getCompanyId());

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.NotFound.USER;

      } catch (SQLException sqle) {
         log.error("Failed to update user", sqle);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse updatePassword(PasswordDTO passwordDTO, AuthUser authUser) {
      final String query = "update user " + "set password_salt=?, password_hash=? " + "where id=?"
            + "  and company_id=?";

      try (Connection con = dbUtils.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

         int i = 0;
         final String salt = codeGenerator.generateSalt();

         pst.setString(++i, salt);
         pst.setString(++i, BCrypt.hashpw(passwordDTO.getPassword(), salt));
         pst.setLong(++i, authUser.getId());
         pst.setLong(++i, authUser.getCompanyId());

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.NotFound.USER;

      } catch (Exception e) {
         log.error("Failed to update user", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse setActiveWorkspace(Long workspaceId) {
      ServiceResponse res = wsRepository.findById(workspaceId);
      if (res.isOK()) {
         final String query = "update user " + "set workspace_id=? " + "where id=?" + "  and company_id=?";

         try (Connection con = dbUtils.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setLong(++i, workspaceId);
            pst.setLong(++i, UserInfo.getUserId());
            pst.setLong(++i, UserInfo.getCompanyId());

            if (pst.executeUpdate() > 0)
               return Responses.OK;
            else
               return Responses.NotFound.WORKSPACE;

         } catch (Exception e) {
            log.error("Failed to update user's workspace", e);
            return Responses.ServerProblem.EXCEPTION;
         }
      } else {
         return Responses.NotFound.WORKSPACE;
      }
   }

   public ServiceResponse deleteById(Long id) {
      boolean result = dbUtils.executeQuery(
            String.format("delete from user " + "where id = %d " + "  and company_id = %d " + "  and role != '%s'", id,
                  UserInfo.getCompanyId(), Role.ADMIN.name()),
            "Failed to delete user with id: " + id);

      if (result) {
         return Responses.OK;
      }
      return Responses.NotFound.USER;
   }

   public ServiceResponse toggleStatus(Long id) {
      boolean result = dbUtils.executeQuery(
            String.format("update user " + "set active = not active " + "where id = %d " + "  and company_id = %d "
                  + "  and role != '%s'", id, UserInfo.getCompanyId(), Role.ADMIN.name()),
            "Failed to toggle user status! id: " + id);

      if (result) {
         return Responses.OK;
      }
      return Responses.NotFound.USER;
   }

   public ServiceResponse setLastCompanyId(Long userId, Long companyId) {
      try (Connection con = dbUtils.getConnection();
            PreparedStatement pst = con.prepareStatement("update user set last_company_id=? where id=?")) {

         pst.setLong(1, companyId);
         pst.setLong(2, userId);

         if (pst.executeUpdate() > 0) {
            dbUtils.commit(con);
            return Responses.OK;
         } else {
            dbUtils.rollback(con);
            return Responses.NotFound.USER;
         }
      } catch (SQLException e) {
         log.error("Failed to set users's last company. UserId: " + userId + ", CompanyId: " + companyId, e);
      }
   }

   private User map(ResultSet rs) {
      try {
         User model = new User();
         model.setId(rs.getLong("id"));
         model.setEmail(rs.getString("email"));
         model.setName(rs.getString("name"));
         model.setLastCompanyId(rs.getLong("last_company_id"));
         model.setCreatedAt(rs.getDate("created_at"));
         model.setPasswordHash(rs.getString("password_hash"));
         model.setPasswordSalt(rs.getString("password_salt"));
         return model;
      } catch (SQLException e) {
         log.error("Failed to set user's properties", e);
      }
      return null;
   }

   private void initFields(List<User> users) {
      if (users != null && users.size() > 0) {
         for (User user : users) {
            user.setLastCompanyId(null);
            user.setPasswordSalt(null);
            user.setPasswordHash(null);
         }
      }
   }

}
