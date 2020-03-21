package io.inprice.scrapper.api.app.company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.plan.PlanStatus;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Props;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.utils.CodeGenerator;
import jodd.util.BCrypt;

public class CompanyRepository {

   private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

   private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

   public ServiceResponse findById(Long id) {
      Company model = dbUtils.findSingle("select * from company where id=" + id, CompanyRepository::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.DataProblem.DB_PROBLEM;
   }

   public ServiceResponse findByAdminId(Long adminId) {
      Company model = dbUtils.findSingle("select * from company where admin_id=" + adminId, CompanyRepository::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.COMPANY;
   }

   /**
    * Three insert operations happen during a new company creation:
    * admin user, company and member
    */
   public ServiceResponse insert(RegisterDTO dto) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      Connection con = null;

      // admin user insertion
      try {
         con = dbUtils.getTransactionalConnection();

         if (dto.getUserId() == null) {
            //last control to prevent duplication
            ServiceResponse found = userRepository.findByEmail(con, dto.getEmail());
            if (found.isOK()) {
               User user = found.getData();
               dto.setUserId(user.getId());
               dto.setUserName(user.getName());
            } else {
               final String salt = codeGenerator.generateSalt();
               final String userInsertQuery = "insert into user "
                     + "(email, name, password_salt, password_hash) "
                     + "values (?, ?, ?, ?) ";

               try (PreparedStatement pst = con.prepareStatement(userInsertQuery, Statement.RETURN_GENERATED_KEYS)) {
                  int i = 0;
                  pst.setString(++i, dto.getEmail());
                  pst.setString(++i, dto.getUserName());
                  pst.setString(++i, salt);
                  pst.setString(++i, BCrypt.hashpw(dto.getPassword(), salt));

                  if (pst.executeUpdate() > 0) {
                     try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                           dto.setUserId(generatedKeys.getLong(1));
                        }
                     }
                  }
               }
            }
         }

         if (dto.getUserId() != null) {
            Long companyId = null;
            //company insertion
            try (PreparedStatement pst = con.prepareStatement(
                  "insert into company (admin_id, name, sector, website, country) values (?, ?, ?, ?, ?) ",
                  Statement.RETURN_GENERATED_KEYS)) {
               int i = 0;
               pst.setLong(++i, dto.getUserId());
               pst.setString(++i, dto.getCompanyName());
               pst.setString(++i, dto.getSector());
               pst.setString(++i, dto.getWebsite());
               pst.setString(++i, dto.getCountry());

               if (pst.executeUpdate() > 0) {
                  try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                     if (generatedKeys.next()) {
                        companyId = generatedKeys.getLong(1);
                     }
                  }
               }
            }

            // mamber insertion
            if (companyId != null) {
               try (PreparedStatement pst = con.prepareStatement(
                     "insert into member (email, company_id, role, status) values (?, ?, ?, ?) ")) {
                  int i = 0;
                  pst.setString(++i, dto.getEmail());
                  pst.setLong(++i, companyId);
                  pst.setString(++i, UserRole.ADMIN.name());
                  pst.setString(++i, UserStatus.JOINED.name());

                  if (pst.executeUpdate() > 0) {
                     response = Responses.OK;
                  }
               }
            }
         } else {
            response = Responses.NotFound.USER;
         }

         if (Responses.OK.equals(response)) {
            dbUtils.commit(con);
         } else {
            dbUtils.rollback(con);
         }

      } catch (SQLException e) {
         if (con != null) {
            dbUtils.rollback(con);
         }
         log.error("Failed to register a new company. " + dto, e);
      } finally {
         if (con != null) {
            dbUtils.close(con);
         }
      }

      return response;
   }

   public ServiceResponse update(CompanyDTO companyDTO) {
      try (Connection con = dbUtils.getConnection();
            PreparedStatement pst = con
                  .prepareStatement("update company set name=?, country=? where id=? and owner_id=?")) {

         int i = 0;
         pst.setString(++i, companyDTO.getName());
         pst.setString(++i, companyDTO.getCountry());
         pst.setLong(++i, UserInfo.getCompanyId());
         pst.setLong(++i, UserInfo.getUserId());

         if (pst.executeUpdate() <= 0) {
            return Responses.NotFound.COMPANY;
         }

         return Responses.OK;

      } catch (SQLException e) {
         log.error("Failed to update company. " + companyDTO, e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   private static Company map(ResultSet rs) {
      try {
         Company model = new Company();
         model.setId(rs.getLong("id"));
         model.setName(rs.getString("name"));
         model.setCountry(rs.getString("country"));
         model.setOwnerId(rs.getLong("owner_id"));
         model.setPlanId(rs.getLong("plan_id"));
         model.setPlanStatus(PlanStatus.valueOf(rs.getString("plan_status")));
         model.setDueDate(rs.getDate("due_date"));
         model.setRetry(rs.getInt("retry"));
         model.setLastCollectingTime(rs.getDate("last_collecting_time"));
         model.setLastCollectingStatus(rs.getBoolean("last_collecting_status"));
         model.setCreatedAt(rs.getDate("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set company's properties", e);
      }
      return null;
   }

}
