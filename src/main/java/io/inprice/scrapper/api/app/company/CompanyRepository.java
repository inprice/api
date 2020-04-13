package io.inprice.scrapper.api.app.company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.app.member.MemberStatus;
import io.inprice.scrapper.api.app.plan.PlanStatus;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserCompany;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.CodeGenerator;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import jodd.util.BCrypt;

public class CompanyRepository {

   private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

   private final Database db = Beans.getSingleton(Database.class);
   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

   public ServiceResponse findById(Long id) {
      Company model = db.findSingle("select * from company where id=" + id, CompanyRepository::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.DataProblem.DB_PROBLEM;
   }

   public ServiceResponse findByAdminId(Long adminId) {
      Company model = db.findSingle("select * from company where admin_id=" + adminId, CompanyRepository::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.COMPANY;
   }

   public ServiceResponse findByCompanyNameAndAdminId(Connection con, String name, Long adminId) {
      Company model = 
         db.findSingle(
            con,
            String.format("select * from company where name='%s' and admin_id=%d", SqlHelper.clear(name), adminId), CompanyRepository::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.COMPANY;
   }

   /**
    * Three insert operations happen during a new company creation:
    * admin user, company and member
    */
   public ServiceResponse insert(RegisterDTO dto, String token) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      Connection con = null;

      // admin user insertion
      try {
         con = db.getTransactionalConnection();
         User user = null;

         if (dto.getUserId() == null) {
            // last duplication control
            ServiceResponse found = userRepository.findByEmail(con, dto.getEmail());
            if (found.isOK()) {
               user = found.getData();
               dto.setUserId(user.getId());
               dto.setUserName(user.getName());
            } else {           

               final String salt = codeGenerator.generateSalt();
               final String userInsertQuery = "insert into user (email, name, password_salt, password_hash) values (?, ?, ?, ?) ";

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

            ServiceResponse found = findByCompanyNameAndAdminId(con, dto.getCompanyName().trim(), dto.getUserId());
            if (! found.isOK()) {
            
               Long companyId = null;

               //company insertion
               try (PreparedStatement pst = con.prepareStatement(
                     "insert into company (admin_id, name, sector, website, country) values (?, ?, ?, ?, ?) ",
                     Statement.RETURN_GENERATED_KEYS)) {
                  int i = 0;
                  pst.setLong(++i, dto.getUserId());
                  pst.setString(++i, dto.getCompanyName().trim());
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
                        "insert into member (user_id, email, company_id, role, status) values (?, ?, ?, ?, ?) ")) {
                     int i = 0;
                     pst.setLong(++i, dto.getUserId());
                     pst.setString(++i, dto.getEmail());
                     pst.setLong(++i, companyId);
                     pst.setString(++i, MemberRole.ADMIN.name());
                     pst.setString(++i, MemberStatus.JOINED.name());

                     if (pst.executeUpdate() > 0) {
                        log.info("A new user just registered a new company -> " + dto.toString());
                        response = Responses.OK;
                     }
                  }
               }

               if (Responses.OK.equals(response)) {
                  TokenService.remove(TokenType.REGISTER_REQUEST, token);
                  //returning user for auto login
                  if (user == null) {
                     user = new User();
                     user.setId(dto.getUserId());
                     user.setEmail(dto.getEmail());
                     user.setName(dto.getUserName());
                     user.setCreatedAt(new Date());

                     user.setCompanies(
                        Arrays.asList(new UserCompany(companyId, dto.getCompanyName(), MemberRole.ADMIN))
                     );
                  }
                  response = new ServiceResponse(user);

                  db.commit(con);
               } else {
                  db.rollback(con);
               }

            } else {
               response = Responses.Already.Defined.COMPANY;
            }
         } else {
            response = Responses.NotFound.USER;
         }

      } catch (SQLException e) {
         if (con != null) {
            db.rollback(con);
         }
         log.error("Failed to register a new company. " + dto, e);
      } finally {
         if (con != null) {
            db.close(con);
         }
      }

      return response;
   }

   public ServiceResponse update(CompanyDTO dto) {
      try (Connection con = db.getConnection();
            PreparedStatement pst = con
                  .prepareStatement("update company set name=?, website=?, sector=?, country=? where id=? and admin_id=?")) {

         int i = 0;
         pst.setString(++i, dto.getCompanyName());
         pst.setString(++i, dto.getWebsite());
         pst.setString(++i, dto.getSector());
         pst.setString(++i, dto.getCountry());
         pst.setLong(++i, CurrentUser.getCompanyId());
         pst.setLong(++i, CurrentUser.getUserId());

         if (pst.executeUpdate() <= 0) {
            return Responses.NotFound.COMPANY;
         }

         return Responses.OK;

      } catch (SQLException e) {
         log.error("Failed to update company. " + dto, e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   private static Company map(ResultSet rs) {
      try {
         Company model = new Company();
         model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
         model.setName(rs.getString("name"));
         model.setCountry(rs.getString("country"));
         model.setAdminId(RepositoryHelper.nullLongHandler(rs, "admin_id"));
         model.setPlanId(RepositoryHelper.nullLongHandler(rs, "plan_id"));
         model.setPlanStatus(PlanStatus.valueOf(rs.getString("plan_status")));
         model.setDueDate(rs.getTimestamp("due_date"));
         model.setRetry(rs.getInt("retry"));
         model.setLastCollectingTime(rs.getTimestamp("last_collecting_time"));
         model.setLastCollectingStatus(rs.getBoolean("last_collecting_status"));
         model.setCreatedAt(rs.getTimestamp("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set company's properties", e);
      }
      return null;
   }

}
