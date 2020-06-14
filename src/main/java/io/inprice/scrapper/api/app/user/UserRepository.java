package io.inprice.scrapper.api.app.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.helpers.CodeGenerator;
import io.inprice.scrapper.common.helpers.RepositoryHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.UserStatus;
import io.inprice.scrapper.common.models.User;
import io.inprice.scrapper.common.utils.DateUtils;
import jodd.util.BCrypt;

public class UserRepository {

  private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

  private final Database db = Beans.getSingleton(Database.class);
  private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

  public ServiceResponse findById(Long id) {
    User model = db.findSingle(String.format("select * from user where id = %d", id), this::map);
    if (model != null) return new ServiceResponse(model);
    return Responses.NotFound.USER;
  }

  public ServiceResponse findByEmail(String email) {
    return findByEmail(email, false);
  }

  public ServiceResponse findByEmail(String email, boolean passwordFields) {
    try (Connection con = db.getConnection()) {
      return findByEmail(con, email, passwordFields);
    } catch (SQLException e) {
      log.error("Failed to insert user", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public ServiceResponse findByEmail(Connection con, String email) {
    return findByEmail(con, email, false);
  }

  public ServiceResponse findByEmail(Connection con, String email, boolean passwordFields) {
    User model = db.findSingle(con, String.format("select * from user where email = '%s'", email), this::map);
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
      final String query = "insert into user (email, name, timezone, password_salt, password_hash) values (?, ?, ?, ?, ?) ";

      try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
         final String salt = codeGenerator.generateSalt();
         
         int i = 0;
         pst.setString(++i, dto.getEmail());
         pst.setString(++i, dto.getName());
         pst.setString(++i, dto.getTimezone());
         pst.setString(++i, salt);
         pst.setString(++i, BCrypt.hashpw(dto.getPassword(), salt));

          if (pst.executeUpdate() > 0) {
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
              if (generatedKeys.next()) {
                User user = new User();
                user.setId(generatedKeys.getLong(1));
                user.setEmail(dto.getEmail());
                user.setName(dto.getName());
                user.setTimezone(dto.getTimezone());
                return new ServiceResponse(user);
              }
            }
          }
          return Responses.DataProblem.DB_PROBLEM;

      } catch (SQLIntegrityConstraintViolationException ie) {
         log.error("Failed to insert user: " + ie.getMessage());
         return Responses.DataProblem.INTEGRITY_PROBLEM;
      } catch (SQLException e) {
         log.error("Failed to insert user", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

  public ServiceResponse update(UserDTO dto) {
    final String query = "update user set name=?, timezone=? where id=?";

    try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

      int i = 0;
      pst.setString(++i, dto.getName());
      pst.setString(++i, dto.getTimezone());
      pst.setLong(++i, CurrentUser.getUserId());

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
    return updatePassword(CurrentUser.getUserId(), password);
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

  public ServiceResponse findMemberships() {
    String query = 
      "select mem.id, c.name, mem.role, mem.status, mem.updated_at " + 
      "from membership as mem " + 
      "left join company as c on c.id = mem.company_id " + 
      "where email=? " + 
      "  and company_id!=? " + 
      "  and status in (?, ?) " + 
      "order by status, updated_at desc";

    try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, CurrentUser.getEmail());
      pst.setLong(++i, CurrentUser.getCompanyId());
      pst.setString(++i, UserStatus.JOINED.name());
      pst.setString(++i, UserStatus.LEFT.name());

      try (ResultSet rs = pst.executeQuery()) {
        List<Map<String, String>> data = new ArrayList<>();
        while (rs.next()) {
          Map<String, String> map = new HashMap<>(5);
          map.put("id", rs.getString("id"));
          map.put("company", rs.getString("name"));
          map.put("role", rs.getString("role"));
          map.put("status", rs.getString("status"));
          map.put("date", DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
          data.add(map);
        }
        return new ServiceResponse(data);
      }
    } catch (SQLException e) {
      log.error("Failed to get user memberships.", e);
      return Responses.DataProblem.DB_PROBLEM;
    }
  }

  public ServiceResponse leaveMembership(Long id) {
    final String query = "update membership set status=?, user_id=?, updated_at=now() where id=? and email=? and status=?";

    try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, UserStatus.LEFT.name());
      pst.setLong(++i, CurrentUser.getUserId());
      pst.setLong(++i, id);
      pst.setString(++i, CurrentUser.getEmail());
      pst.setString(++i, UserStatus.JOINED.name());

      if (pst.executeUpdate() > 0)
        return Responses.OK;
      else
        return Responses.NotFound.USER;

    } catch (SQLException sqle) {
      log.error("Failed to leave membership. User: " + CurrentUser.getUserId(), sqle);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public ServiceResponse findActiveInvitations() {
    String query = 
      "select mem.id, c.name, mem.role, mem.created_at " + 
      "from membership as mem " + 
      "left join company as c on c.id = mem.company_id " + 
      "where email=? " + 
      "  and status=? " + 
      "order by created_at desc";

    try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
      pst.setString(1, CurrentUser.getEmail());
      pst.setString(2, UserStatus.PENDING.name());

      try (ResultSet rs = pst.executeQuery()) {
        List<Map<String, String>> data = new ArrayList<>();
        while (rs.next()) {
          Map<String, String> map = new HashMap<>(4);
          map.put("id", rs.getString("id"));
          map.put("company", rs.getString("name"));
          map.put("role", rs.getString("role"));
          map.put("date", DateUtils.formatLongDate(rs.getTimestamp("created_at")));
          data.add(map);
        }
        return new ServiceResponse(data);
      }
    } catch (SQLException e) {
      log.error("Failed to get user invitations.", e);
      return Responses.DataProblem.DB_PROBLEM;
    }
  }

  public ServiceResponse acceptInvitation(Long id) {
    return processInvitation(id, UserStatus.PENDING, UserStatus.JOINED);
  }

  public ServiceResponse rejectInvitation(Long id) {
    return processInvitation(id, UserStatus.JOINED, UserStatus.LEFT);
  }

  private ServiceResponse processInvitation(Long id, UserStatus fromStatus, UserStatus toStatus) {
    final String query = "update membership set status=?, user_id=?, updated_at=now() where id=? and email=? and status=?";

    try (Connection con = db.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, toStatus.name());
      pst.setLong(++i, CurrentUser.getUserId());
      pst.setLong(++i, id);
      pst.setString(++i, CurrentUser.getEmail());
      pst.setString(++i, fromStatus.name());

      if (pst.executeUpdate() > 0)
        return Responses.OK;
      else
        return Responses.NotFound.USER;

    } catch (SQLException sqle) {
      log.error("Failed to set invitation status of user: " + CurrentUser.getUserId() + " from " + fromStatus + " to "
          + toStatus, sqle);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  private User map(ResultSet rs) {
    try {
      User model = new User();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setEmail(rs.getString("email"));
      model.setName(rs.getString("name"));
      model.setTimezone(rs.getString("timezone"));
      model.setPasswordHash(rs.getString("password_hash"));
      model.setPasswordSalt(rs.getString("password_salt"));
      model.setCreatedAt(rs.getTimestamp("created_at"));
      return model;
    } catch (SQLException e) {
      log.error("Failed to set user's properties", e);
    }
    return null;
  }

}
