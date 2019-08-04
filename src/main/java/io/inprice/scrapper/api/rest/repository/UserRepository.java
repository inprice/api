package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
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

    public Response<User> findById(Long id, boolean passwordFields) {
        User model = dbUtils.findSingle(String.format("select * from user where id = %d", id), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new Response<>(model);
        } else {
            return Responses.NOT_FOUND("User");
        }
    }

    public Response<User> getList(long companyId) {
        List<User> users = dbUtils.findMultiple(
            String.format(
                "select * from user " +
                "where user_type != '%s' " +
                "  and company_id = %d " +
                "order by full_name", UserType.ADMIN, companyId), this::map);

        if (users != null && users.size() > 0) {
            return new Response<>(users);
        }
        return Responses.NOT_FOUND("User");
    }

    public Response<User> findByEmail(String email, boolean passwordFields) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s'", email), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new Response<>(model);
        } else {
            return Responses.NOT_FOUND("User");
        }
    }

    public Response<User> findByEmailForUpdateCheck(String email, long userId) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s' and id != %d", email, userId), this::map);
        if (model != null) {
            model.setPasswordSalt(null);
            model.setPasswordHash(null);
            return new Response<>(model);
        } else {
            return Responses.NOT_FOUND("User");
        }
    }

    public Response<User> insert(UserDTO userDTO) {
        final String query =
            "insert into user " +
            "(user_type, full_name, email, password_salt, password_hash, company_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?) ";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            //never trust client side!!!
            UserType ut = userDTO.getType();
            if (ut == null || UserType.ADMIN.equals(ut)) ut = UserType.USER;

            int i = 0;
            final String salt = codeGenerator.generateSalt();

            pst.setString(++i, ut.name());
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());
            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(userDTO.getPassword(), salt));
            pst.setLong(++i, userDTO.getCompanyId());

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

    public Response<User> update(Claims claims, UserDTO userDTO, boolean byAdmin, boolean passwordWillBeUpdate) {
        final String query = String.format(
            "update user " +
            "set full_name=?, email=? " +
            (byAdmin ? ", user_type=?, active=?" : "") +
            (passwordWillBeUpdate ? ", password_salt=?, password_hash=?" : "") +
            " where id=?");

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());

            if (byAdmin) {
                UserType ut = userDTO.getType();
                if (ut == null || UserType.ADMIN.equals(ut)) ut = UserType.USER;

                pst.setString(++i, ut.name());
                pst.setBoolean(++i, (userDTO.getActive() != null ? userDTO.getActive() : Boolean.TRUE));
            }

            if (passwordWillBeUpdate) {
                final String salt = codeGenerator.generateSalt();
                pst.setString(++i, salt);
                pst.setString(++i, BCrypt.hashpw(userDTO.getPassword(), salt));
            }

            pst.setLong(++i, userDTO.getId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.NOT_FOUND("User");

        } catch (SQLException sqle) {
            log.error("Failed to update user", sqle);
            return Responses.SERVER_ERROR;
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response<User> updatePassword(Claims claims, PasswordDTO passwordDTO) {
        final String query = String.format(
            "update user " +
            "set password_salt=?, password_hash=? " +
            "where id=?");

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            final String salt = codeGenerator.generateSalt();

            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(passwordDTO.getPassword(), salt));
            pst.setLong(++i, claims.getUserId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (Exception e) {
            log.error("Failed to update user", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response<User> deleteById(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                    "delete from user " +
                        "where id = %d " +
                        "  and user_type != '%s'", id, UserType.ADMIN.name()),
                "Failed to delete user with id: " + id);

        if (result) return Responses.OK;

        return Responses.NOT_FOUND("User");
    }

    public Response<User> toggleStatus(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                    "update user " +
                        "set active = not active " +
                        "where id = %d " +
                        "  and user_type != '%s'", id, UserType.ADMIN.name()),
        "Failed to toggle user status! id: " + id);

        if (result) return Responses.OK;

        return Responses.NOT_FOUND("User");
    }

    public Response<User> setDefaultWorkspace(Claims claims, Long wsId) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                    "update user " +
                        "set default_workspace_id = %d " +
                        "where id = %d " +
                        "  and company_id = %d ", wsId, claims.getUserId(), claims.getCompanyId()),
        "Failed to set default workspace! User Id: " + claims.getUserId() + ", Workspace Id: " + wsId);

        if (result) return Responses.OK;

        return Responses.NOT_FOUND("Workspace or User");
    }

    private User map(ResultSet rs) {
        try {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setActive(rs.getBoolean("active"));
            user.setUserType(UserType.valueOf(rs.getString("user_type")));
            user.setFullName(rs.getString("full_name"));
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