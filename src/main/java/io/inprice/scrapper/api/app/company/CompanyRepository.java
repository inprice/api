package io.inprice.scrapper.api.app.company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.plan.PlanStatus;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.consts.Responses;
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

  public ServiceResponse findByNameAndAdminId(Connection con, String name, Long adminId) {
    Company model = db.findSingle(con,
        String.format("select * from company where name='%s' and admin_id=%d", SqlHelper.clear(name.trim()), adminId),
        CompanyRepository::map);
    if (model != null)
      return new ServiceResponse(model);
    else
      return Responses.NotFound.COMPANY;
  }

  /**
   * Three insert operations happen during a new company creation: admin user,
   * company and member
   */
  public ServiceResponse insert(RegisterDTO dto, String token) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;

    // admin user insertion
    try {
      con = db.getTransactionalConnection();
      User user = null;

      if (dto.getUserId() == null) {
        // duplication control
        ServiceResponse found = userRepository.findByEmail(con, dto.getEmail());
        if (found.isOK()) {
          user = found.getData();
          dto.setUserId(user.getId());
          dto.setUserName(user.getName());
          dto.setTimezone(user.getTimezone());
        } else {

          final String salt = codeGenerator.generateSalt();
          final String userInsertQuery = "insert into user (email, name, timezone, password_salt, password_hash) values (?, ?, ?, ?, ?) ";

          try (PreparedStatement pst = con.prepareStatement(userInsertQuery, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setString(++i, dto.getEmail());
            pst.setString(++i, dto.getUserName());
            pst.setString(++i, dto.getTimezone());
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

        ServiceResponse found = findByNameAndAdminId(con, dto.getCompanyName(), dto.getUserId());
        if (!found.isOK()) {

          Long companyId = null;

          // company insertion
          try (PreparedStatement pst = con.prepareStatement(
              "insert into company (admin_id, name) values (?, ?) ",
              Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setLong(++i, dto.getUserId());
            pst.setString(++i, dto.getCompanyName().trim());

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
                "insert into membership (user_id, email, company_id, role, status, updated_at) values (?, ?, ?, ?, ?, now()) ")) {
              int i = 0;
              pst.setLong(++i, dto.getUserId());
              pst.setString(++i, dto.getEmail());
              pst.setLong(++i, companyId);
              pst.setString(++i, UserRole.ADMIN.name());
              pst.setString(++i, UserStatus.JOINED.name());

              if (pst.executeUpdate() > 0) {
                log.info("A new user just registered a new company -> " + dto.toString());
                res = Responses.OK;
              }
            }
          }

          if (Responses.OK.equals(res)) {
            TokenService.remove(TokenType.REGISTER_REQUEST, token);
            // returning user for auto login
            if (user == null) {
              user = new User();
              user.setId(dto.getUserId());
              user.setEmail(dto.getEmail());
              user.setName(dto.getUserName());
              user.setTimezone(dto.getTimezone());
              user.setCreatedAt(new Date());
            }
            res = new ServiceResponse(user);

            db.commit(con);
          } else {
            db.rollback(con);
          }

        } else {
          res = Responses.Already.Defined.COMPANY;
        }
      } else {
        res = Responses.NotFound.USER;
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

    return res;
  }

  public ServiceResponse create(String name) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;

    try {
      con = db.getTransactionalConnection();

      ServiceResponse found = findByNameAndAdminId(con, name.trim(), CurrentUser.getUserId());
      if (!found.isOK()) {

        Long companyId = null;

        try (PreparedStatement pst = con.prepareStatement(
            "insert into company (admin_id, name) values (?, ?) ", Statement.RETURN_GENERATED_KEYS)) {
          int i = 0;
          pst.setLong(++i, CurrentUser.getUserId());
          pst.setString(++i, name.trim());

          if (pst.executeUpdate() > 0) {
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
              if (generatedKeys.next()) {
                companyId = generatedKeys.getLong(1);
              }
            }
          }
        }

        if (companyId != null) {
          try (PreparedStatement pst = con.prepareStatement(
              "insert into membership (user_id, email, company_id, role, status, updated_at) values (?, ?, ?, ?, ?, now()) ")) {
            int i = 0;
            pst.setLong(++i, CurrentUser.getUserId());
            pst.setString(++i, CurrentUser.getEmail());
            pst.setLong(++i, companyId);
            pst.setString(++i, UserRole.ADMIN.name());
            pst.setString(++i, UserStatus.JOINED.name());

            if (pst.executeUpdate() > 0) {
              log.info("A new company just registered -> " + name);
              res = Responses.OK;
            }
          }
        }

        if (res.isOK()) {
          db.commit(con);
        } else {
          db.rollback(con);
        }

      } else {
        res = Responses.Already.Defined.COMPANY;
      }

    } catch (SQLException e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to create a new company. " + name, e);
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

  public ServiceResponse update(String name) {
    try (Connection con = db.getConnection();
        PreparedStatement pst = con
            .prepareStatement("update company set name=? where id=? and admin_id=?")) {

      int i = 0;
      pst.setString(++i, name.trim());
      pst.setLong(++i, CurrentUser.getCompanyId());
      pst.setLong(++i, CurrentUser.getUserId());

      if (pst.executeUpdate() <= 0) {
        return Responses.NotFound.COMPANY;
      }

      return Responses.OK;

    } catch (SQLException e) {
      log.error("Failed to update company. " + name, e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  private static Company map(ResultSet rs) {
    try {
      Company model = new Company();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setName(rs.getString("name"));
      model.setCurrencyCode(rs.getString("currency_code"));
      model.setCurrencyFormat(rs.getString("currency_format"));
      model.setAdminId(RepositoryHelper.nullLongHandler(rs, "admin_id"));
      model.setPlanId(RepositoryHelper.nullIntegerHandler(rs, "plan_id"));
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
