package io.inprice.api.app.company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.token.TokenService;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.app.user.UserRepository;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CreateCompanyDTO;
import io.inprice.api.dto.RegisterDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.CodeGenerator;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.User;
import jodd.util.BCrypt;

public class CompanyRepository {

  private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

  private final Database db = Beans.getSingleton(Database.class);
  private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
  private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

  public ServiceResponse findById(Long id) {
    Company model = db.findSingle("select * from company where id=" + id, this::map);
    if (model != null)
      return new ServiceResponse(model);
    else
      return Responses.DataProblem.DB_PROBLEM;
  }

  public Company findById(Connection con, Long id) {
    return db.findSingle(con, "select * from company where id=" + id, this::map);
  }

  public Company findBySubsCustomerId(Connection con, String custId) {
    return db.findSingle(con, "select * from company where subs_customer_id='" + custId + "'", this::map);
  }

  public ServiceResponse findByAdminId(Connection con, Long adminId) {
    Company model = db.findSingle(con, "select * from company where admin_id=" + adminId, this::map);
    if (model != null)
      return new ServiceResponse(model);
    else
      return Responses.NotFound.COMPANY;
  }

  public ServiceResponse findByNameAndAdminId(Connection con, String name, Long adminId) {
    Company model = db.findSingle(con,
        String.format("select * from company where name='%s' and admin_id=%d", SqlHelper.clear(name.trim()), adminId),
        this::map);
    if (model != null)
      return new ServiceResponse(model);
    else
      return Responses.NotFound.COMPANY;
  }

  /**
   * Three insert operations happen during a new company creation: admin user,
   * company and member
   */
  public ServiceResponse insert(RegisterDTO dto, Map<String, String> clientInfo, String token) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;

    // admin user insertion
    try {
      con = db.getTransactionalConnection();
      User user = null;

      // duplication control
      ServiceResponse found = userRepository.findByEmail(con, dto.getEmail());
      if (found.isOK()) {
        user = found.getData();
      } else {

        user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getEmail().split("@")[0]);
        user.setTimezone(clientInfo.get(Consts.TIMEZONE));
        user.setCreatedAt(new Date());

        final String salt = codeGenerator.generateSalt();
        final String userInsertQuery = "insert into user (email, name, timezone, password_salt, password_hash) values (?, ?, ?, ?, ?) ";

        try (PreparedStatement pst = con.prepareStatement(userInsertQuery, Statement.RETURN_GENERATED_KEYS)) {
          int i = 0;
          pst.setString(++i, user.getEmail());
          pst.setString(++i, user.getName());
          pst.setString(++i, clientInfo.get(Consts.TIMEZONE));
          pst.setString(++i, salt);
          pst.setString(++i, BCrypt.hashpw(dto.getPassword(), salt));

          if (pst.executeUpdate() > 0) {
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
              if (generatedKeys.next()) {
                user.setId(generatedKeys.getLong(1));
              }
            }
          }
        }
      }

      if (user.getId() != null) {

        found = findByNameAndAdminId(con, dto.getCompanyName(), user.getId());
        if (!found.isOK()) {

          Long companyId = null;

          // company insertion
          try (PreparedStatement pst = con.prepareStatement(
              "insert into company (admin_id, name, currency_code, currency_format) values (?, ?, ?, ?) ",
              Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setLong(++i, user.getId());
            pst.setString(++i, dto.getCompanyName().trim());
            pst.setString(++i, clientInfo.get(Consts.CURRENCY_CODE));
            pst.setString(++i, clientInfo.get(Consts.CURRENCY_FORMAT));

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
              pst.setLong(++i, user.getId());
              pst.setString(++i, user.getEmail());
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

  public ServiceResponse create(CreateCompanyDTO dto) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      ServiceResponse found = findByNameAndAdminId(con, dto.getName().trim(), CurrentUser.getUserId());
      if (!found.isOK()) {

        Long companyId = null;

        try (PreparedStatement pst = con.prepareStatement(
            "insert into company (admin_id, name, currency_code, currency_format) values (?, ?, ?, ?) ", Statement.RETURN_GENERATED_KEYS)) {
          int i = 0;
          pst.setLong(++i, CurrentUser.getUserId());
          pst.setString(++i, dto.getName().trim());
          pst.setString(++i, dto.getCurrencyCode());
          pst.setString(++i, dto.getCurrencyFormat());

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
              log.info("A new company just registered -> " + dto.toString());
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
      log.error("Failed to create a new company. " + dto.toString(), e);
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

  public ServiceResponse update(CreateCompanyDTO dto) {
    try (Connection con = db.getConnection();
        PreparedStatement pst = con
            .prepareStatement("update company set name=?, currency_code=?, currency_format=? where id=? and admin_id=?")) {

      int i = 0;
      pst.setString(++i, dto.getName().trim());
      pst.setString(++i, dto.getCurrencyCode());
      pst.setString(++i, dto.getCurrencyFormat());
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

  public ServiceResponse deleteEverything(String password) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      ServiceResponse res = userRepository.findByEmail(con, CurrentUser.getEmail(), true);
      if (res.isOK()) {

        User user = res.getData();
        String phash = BCrypt.hashpw(password, user.getPasswordSalt());
        if (phash.equals(user.getPasswordHash())) {

          res = findByAdminId(con, CurrentUser.getCompanyId());
          if (res.isOK()) {
            Company company = res.getData();

            if (company.getAdminId().equals(user.getId())) {

              List<String> hashList = db.findMultiple(con,
                  String.format("select _hash from user_session where company_id=%d", CurrentUser.getCompanyId()), this::mapForHashField);

              String where = "where company_id=" + CurrentUser.getCompanyId();

              List<String> queries = new ArrayList<>(14);
              queries.add("SET FOREIGN_KEY_CHECKS=0");
              queries.add("delete from competitor_price " + where);
              queries.add("delete from competitor_history " + where);
              queries.add("delete from competitor_spec " + where);
              queries.add("delete from competitor " + where);
              queries.add("delete from product_price " + where);
              queries.add("delete from product " + where);
              queries.add("delete from lookup " + where);
              queries.add("delete from user_session " + where);
              queries.add("delete from membership " + where);
              queries.add("delete from subs_trans " + where);
              queries.add("delete from user where id in (select admin_id from company where id="+CurrentUser.getCompanyId()+")");
              queries.add("delete from company where id="+CurrentUser.getCompanyId());
              queries.add("SET FOREIGN_KEY_CHECKS=1");

              if (hashList != null && hashList.size() > 0) {
                for (String hash : hashList) {
                  RedisClient.removeSesion(hash);
                }
              }

              db.executeBatchQueries(con, queries);
              db.commit(con);

              return Responses.OK;

            } else {
              return Responses.Invalid.COMPANY;
            }
          } else {
            return Responses.PermissionProblem.UNAUTHORIZED;
          }
        } else {
          return Responses.Invalid.USER;
        }
      } else {
        return res;
      }
    } catch (SQLException e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to delete everything for a company. " + CurrentUser.getCompanyName(), e);
      return Responses.ServerProblem.EXCEPTION;
    } finally {
      if (con != null) {
        db.close(con);
      }
    }
  }

  private String mapForHashField(ResultSet rs) {
    try {
      return rs.getString("_hash");
    } catch (SQLException e) {
      log.error("Failed to get _hash field from user_session table", e);
    }
    return null;
  }

  private Company map(ResultSet rs) {
    try {
      Company model = new Company();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setName(rs.getString("name"));
      model.setCurrencyCode(rs.getString("currency_code"));
      model.setCurrencyFormat(rs.getString("currency_format"));
      model.setProductLimit(rs.getInt("product_limit"));
      model.setProductCount(rs.getInt("product_count"));
      model.setAdminId(RepositoryHelper.nullLongHandler(rs, "admin_id"));
      model.setPlanId(RepositoryHelper.nullIntegerHandler(rs, "plan_id"));
      model.setSubsStatus(SubsStatus.valueOf(rs.getString("subs_status")));
      model.setSubsRenewalAt(rs.getTimestamp("subs_renewal_at"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set company's properties", e);
    }
    return null;
  }

}
