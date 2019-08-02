package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
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

    public Response<User> getAll(long companyId) {
        List<User> users = dbUtils.findMultiple(String.format("select * from user where company_id = %d order by email", companyId), this::map);
        if (users != null && users.size() > 0) {
            return new Response(users);
        }
        return Responses.NOT_FOUND("User");
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

    public Response insert(UserDTO userDTO) {
        final String salt = codeGenerator.generateSalt();
        final String query =
            "insert into user " +
            "(user_type, name, email, password_salt, password_hash, company_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?) ";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {


            //never trust client side!!!
            UserType ut = userDTO.getType();
            if (ut == null || UserType.ADMIN.equals(ut)) ut = UserType.USER;

            int i = 0;
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

    public Response updateByAdmin(UserDTO userDTO) {
        final String query =
            "update user " +
            "set active?, name=?, email=?, user_type=? " +
            "where id=?";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            //never trust client side!!!
            UserType ut = userDTO.getType();
            if (UserType.ADMIN.equals(ut)) ut = UserType.USER;

            int i = 0;
            pst.setBoolean(++i, userDTO.getActive());
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());
            pst.setString(++i, ut.name());
            pst.setLong(++i, userDTO.getId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (Exception e) {
            log.error("Failed to update user", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response updateByUser(UserDTO userDTO) {
        final String query =
            "update user " +
            "set name=?, email=? " +
            "where id=?";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            //never trust client side!!!
            UserType ut = userDTO.getType();
            if (UserType.ADMIN.equals(ut)) ut = UserType.USER;

            int i = 0;
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());
            pst.setLong(++i, userDTO.getId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (Exception e) {
            log.error("Failed to update user", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response deleteById(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format("delete from user where id = %d", id),"Failed to delete user with id: " + id);

        if (result) return Responses.OK;

        return Responses.NOT_FOUND("User");
    }

    public Response toggleStatus(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format("update user set active = !active where id = %d and user_type != 'ADMIN'", id),"Failed to toggle user status! id: " + id);

        if (result) return Responses.OK;

        return Responses.NOT_FOUND("User");
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
