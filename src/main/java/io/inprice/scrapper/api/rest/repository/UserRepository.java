package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.utils.CodeGenerator;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
    private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

    public ServiceResponse<User> findById(Long id) {
        return findById(id, false);
    }

    public ServiceResponse<User> findById(Long id, boolean passwordFields) {
        User model =
            dbUtils.findSingle(
                String.format(
                    "select * from user " +
                    "where id = %d " +
                    "  and company_id = %d", id, Context.getCompanyId()), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new ServiceResponse<>(model);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse<User> getList() {
        List<User> users = dbUtils.findMultiple(
            String.format(
                "select * from user " +
                "where user_type != '%s' " +
                "  and company_id = %d " +
                "order by full_name", UserType.ADMIN, Context.getCompanyId()), this::map);

        if (users != null && users.size() > 0) {
            return new ServiceResponse<>(users);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse<User> findByEmail(String email) {
        return findByEmail(email, false);
    }

    public ServiceResponse<User> findByEmail(String email, boolean passwordFields) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s'", email), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new ServiceResponse<>(model);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse<User> findByEmailForUpdateCheck(String email, long userId) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s' and id != %d", email, userId), this::map);
        if (model != null) {
            model.setPasswordSalt(null);
            model.setPasswordHash(null);
            return new ServiceResponse<>(model);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse insert(UserDTO userDTO) {
        final String query =
            "insert into user " +
            "(user_type, full_name, email, password_salt, password_hash, company_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?) ";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            //never trust client side!!!
            UserType ut = userDTO.getType();
            if (ut == null || UserType.ADMIN.equals(ut)) ut = UserType.EDITOR;

            int i = 0;
            final String salt = codeGenerator.generateSalt();

            pst.setString(++i, ut.name());
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());
            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(userDTO.getPassword(), salt));
            pst.setLong(++i, Context.getCompanyId());

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

    public ServiceResponse update(UserDTO userDTO, boolean byAdmin, boolean passwordWillBeUpdate) {
        final String query =
            "update user " +
            "set full_name=?, email=? " +
            (byAdmin ? ", user_type=?" : "") +
            (passwordWillBeUpdate ? ", password_salt=?, password_hash=?" : "") +
            " where id=?" +
            "   and company_id=?";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());

            if (byAdmin) {
                UserType ut = userDTO.getType();
                if (ut == null || UserType.ADMIN.equals(ut)) ut = UserType.EDITOR;
                pst.setString(++i, ut.name());
            }

            if (passwordWillBeUpdate) {
                final String salt = codeGenerator.generateSalt();
                pst.setString(++i, salt);
                pst.setString(++i, BCrypt.hashpw(userDTO.getPassword(), salt));
            }

            pst.setLong(++i, userDTO.getId());
            pst.setLong(++i, Context.getCompanyId());

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
        final String query =
            "update user " +
            "set password_salt=?, password_hash=? " +
            "where id=?" +
            "  and company_id=?";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

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

    public ServiceResponse deleteById(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                "delete from user " +
                    "where id = %d " +
                    "  and company_id = %d " +
                    "  and user_type != '%s'", id, Context.getCompanyId(), UserType.ADMIN.name()),
            "Failed to delete user with id: " + id);

        if (result) {
            return Responses.OK;
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse toggleStatus(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                "update user " +
                    "set active = not active " +
                    "where id = %d " +
                    "  and company_id = %d " +
                    "  and user_type != '%s'", id, Context.getCompanyId(), UserType.ADMIN.name()),
        "Failed to toggle user status! id: " + id);

        if (result) {
            return Responses.OK;
        }
        return Responses.NotFound.USER;
    }

    private User map(ResultSet rs) {
        try {
            User model = new User();
            model.setId(rs.getLong("id"));
            model.setActive(rs.getBoolean("active"));
            model.setUserType(UserType.valueOf(rs.getString("user_type")));
            model.setFullName(rs.getString("full_name"));
            model.setEmail(rs.getString("email"));
            model.setPasswordHash(rs.getString("password_hash"));
            model.setPasswordSalt(rs.getString("password_salt"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setInsertAt(rs.getDate("insert_at"));
            return model;
        } catch (SQLException e) {
            log.error("Failed to set user's properties", e);
        }
        return null;
    }

}
