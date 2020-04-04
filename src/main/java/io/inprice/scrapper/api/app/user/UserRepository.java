package io.inprice.scrapper.api.app.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.helpers.CodeGenerator;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import jodd.util.BCrypt;

public class UserRepository {

   private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

   private final Database db = Beans.getSingleton(Database.class);
   private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

   public ServiceResponse findById(Long id) {
      return findById(id, false);
   }

   public ServiceResponse findById(Long id, boolean passwordFields) {
      User model = db.findSingle(String.format("select * from user where id = %d", id), this::map);
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

   public ServiceResponse findByEmail(Connection con, String email) {
      User model = db.findSingle(con, String.format("select * from user where email = '%s'", email), this::map);
      if (model != null) {
         model.setPasswordSalt(null);
         model.setPasswordHash(null);
         return new ServiceResponse(model);
      }
      return Responses.NotFound.USER;
   }

   public ServiceResponse findByEmail(String email, boolean passwordFields) {
      User model = db.findSingle(String.format("select * from user where email = '%s'", email), this::map);
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
      try (Connection con = db.getConnection()) {
         return insert(con, dto);
      } catch (SQLException e) {
         log.error("Failed to insert user", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse insert(Connection con, UserDTO dto) {
      final String query = "insert into user "
            + "(email, name, last_company_id, password_salt, password_hash) values "
            + "(?, ?, ?, ?, ?) ";

      try (PreparedStatement pst = con.prepareStatement(query)) {
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
      } catch (SQLException e) {
         log.error("Failed to insert user", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse updateName(String name) {
      final String query = "update user set name=? where id=?";

      try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

         int i = 0;
         pst.setString(++i, name);
         pst.setLong(++i, CurrentUser.getId());

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.NotFound.USER;

      } catch (SQLException sqle) {
         log.error("Failed to update user", sqle);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse updatePassword(String password) {
      return updatePassword(CurrentUser.getId(), password);
   }

   public ServiceResponse updatePassword(Long userId, String password) {
      final String query = "update user set password_salt=?, password_hash=? where id=?";

      try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

         int i = 0;
         final String salt = codeGenerator.generateSalt();

         pst.setString(++i, salt);
         pst.setString(++i, BCrypt.hashpw(password, salt));
         pst.setLong(++i, userId);

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.NotFound.USER;

      } catch (Exception e) {
         log.error("Failed to update user", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse updateLastCompany(Long companyId) {
      return updateLastCompany(CurrentUser.getId(), companyId);
   }

   public ServiceResponse updateLastCompany(Long userId, Long companyId) {
      final String query = "update user set last_company_id=? where id=?";

      try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

         int i = 0;
         pst.setLong(++i, companyId);
         pst.setLong(++i, userId);

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.NotFound.COMPANY;

      } catch (Exception e) {
         log.error("Failed to set users's last company. UserId: " + CurrentUser.getId() + ", CompanyId: " + companyId, e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   private User map(ResultSet rs) {
      try {
         User model = new User();
         model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
         model.setEmail(rs.getString("email"));
         model.setName(rs.getString("name"));
         model.setLastCompanyId(RepositoryHelper.nullLongHandler(rs, "last_company_id"));
         model.setPasswordHash(rs.getString("password_hash"));
         model.setPasswordSalt(rs.getString("password_salt"));
         model.setCreatedAt(rs.getDate("created_at"));
         return model;
      } catch (SQLException e) {
         log.error("Failed to set user's properties", e);
      }
      return null;
   }

}
