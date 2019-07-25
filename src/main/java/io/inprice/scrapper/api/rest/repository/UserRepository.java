package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;

import java.sql.*;

public class UserRepository {

    private static final Logger log = new Logger(UserRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public Response insert(User user) {
        final String salt = BCrypt.gensalt(12);
        final String query =
            "insert into user " +
            "(user_type, name, email, password_salt, password_hash, company_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?) ";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, user.getUserType().name());
            pst.setString(++i, user.getName());
            pst.setString(++i, user.getEmail());
            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(user.getPassword(), salt));
            pst.setLong(++i, user.getCompanyId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (SQLIntegrityConstraintViolationException ie) {
            log.error("Failed to insert user: " + ie.getMessage());
            return Responses.SERVER_ERROR;
        } catch (Exception e) {
            log.error("Failed to insert user", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response update(User user) {
        final String query =
            "update user " +
            "set user_type=?, company_id=? " +
            "where email=?";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, user.getUserType().name());
            pst.setLong(++i, user.getCompanyId());
            pst.setString(++i, user.getEmail());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (Exception e) {
            log.error("Failed to update user", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response findById(Long id, boolean passwordFields) {
        User model = dbUtils.findSingle(String.format("select * from user where id = %d", id), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new Response(model);
        } else {
            return Responses.NOT_FOUND("User");
        }
    }

    public Response findByEmail(String email, boolean passwordFields) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s'", email), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new Response(model);
        } else {
            return Responses.NOT_FOUND("User");
        }
    }

    private User map(ResultSet rs) {
        try {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUserType(UserType.valueOf(rs.getString("user_type")));
            user.setEmail(rs.getString("email"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setPasswordSalt(rs.getString("password_salt"));
            user.setCompanyId(rs.getLong("company_id"));
            user.setInsertAt(rs.getDate("insert_at"));
            return user;
        } catch (SQLException e) {
            log.error("Failed to set user's properties", e);
        }
        return null;
    }

}
