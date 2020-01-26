package io.inprice.scrapper.api.rest.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.utils.CodeGenerator;
import io.inprice.scrapper.common.meta.Role;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;

public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
    private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);
    private final WorkspaceRepository wsRepository = Beans.getSingleton(WorkspaceRepository.class);

    public ServiceResponse findById(Long id) {
        return findById(id, false);
    }

    public ServiceResponse findById(Long id, boolean passwordFields) {
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
            return new ServiceResponse(model);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse getList() {
        List<User> users = dbUtils.findMultiple(
            String.format(
                "select * from user " +
                "where role != '%s' " +
                "  and company_id = %d " +
                "order by full_name", Role.admin, Context.getCompanyId()), this::map);

        if (users != null && users.size() > 0) {
            for (User user: users) {
                user.setCompanyId(null);
                user.setWorkspaceId(null);
                user.setPasswordSalt(null);
                user.setPasswordHash(null);
            }
        }
        return new ServiceResponse(users);
    }

    public ServiceResponse findByEmail(String email) {
        return findByEmail(email, false);
    }

    public ServiceResponse findByEmail(String email, boolean passwordFields) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s'", email), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new ServiceResponse(model);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse findByEmailForUpdateCheck(String email, long userId) {
        User model = dbUtils.findSingle(String.format("select * from user where email = '%s' and id != %d", email, userId), this::map);
        if (model != null) {
            model.setPasswordSalt(null);
            model.setPasswordHash(null);
            return new ServiceResponse(model);
        }
        return Responses.NotFound.USER;
    }

    public ServiceResponse insert(UserDTO userDTO) {
        final String query =
            "insert into user " +
            "(role, full_name, email, password_salt, password_hash, company_id, workspace_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?, ?) ";

        final Long masterId = wsRepository.findMasterWsId();

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            //never trust client side!!!
            Role ut = userDTO.getRole();
            if (ut == null || Role.admin.equals(ut)) ut = Role.editor;

            int i = 0;
            final String salt = codeGenerator.generateSalt();

            pst.setString(++i, ut.name());
            pst.setString(++i, userDTO.getFullName());
            pst.setString(++i, userDTO.getEmail());
            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(userDTO.getPassword(), salt));
            pst.setLong(++i, Context.getCompanyId());
            pst.setLong(++i, masterId);

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
            "set full_name=? " +
            (! StringUtils.isBlank(userDTO.getEmail()) ? ", email=? " : "") +
            (byAdmin ? ", role=?" : "") +
            (passwordWillBeUpdate ? ", password_salt=?, password_hash=?" : "") +
            " where id=?" +
            "   and company_id=?";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, userDTO.getFullName());

            if (! StringUtils.isBlank(userDTO.getEmail())) {
                pst.setString(++i, userDTO.getEmail());
            }

            if (byAdmin) {
                Role ut = userDTO.getRole();
                if (ut == null || Role.admin.equals(ut)) ut = Role.editor;
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

    public ServiceResponse setActiveWorkspace(Long workspaceId) {
        ServiceResponse res = wsRepository.findById(workspaceId);
        if (res.isOK()) {
            final String query =
                "update user " +
                "set workspace_id=? " +
                "where id=?" +
                "  and company_id=?";

            try (Connection con = dbUtils.getConnection();
                 PreparedStatement pst = con.prepareStatement(query)) {

                int i = 0;
                pst.setLong(++i, workspaceId);
                pst.setLong(++i, Context.getUserId());
                pst.setLong(++i, Context.getCompanyId());

                if (pst.executeUpdate() > 0)
                    return Responses.OK;
                else
                    return Responses.NotFound.WORKSPACE;

            } catch (Exception e) {
                log.error("Failed to update user's workspace", e);
                return Responses.ServerProblem.EXCEPTION;
            }
        } else {
            return Responses.NotFound.WORKSPACE;
        }
    }

    public ServiceResponse deleteById(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                "delete from user " +
                    "where id = %d " +
                    "  and company_id = %d " +
                    "  and role != '%s'", id, Context.getCompanyId(), Role.admin.name()),
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
                    "  and role != '%s'", id, Context.getCompanyId(), Role.admin.name()),
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
            model.setRole(Role.valueOf(rs.getString("role")));
            model.setFullName(rs.getString("full_name"));
            model.setEmail(rs.getString("email"));
            model.setPasswordHash(rs.getString("password_hash"));
            model.setPasswordSalt(rs.getString("password_salt"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));
            model.setCreatedAt(rs.getDate("created_at"));
            return model;
        } catch (SQLException e) {
            log.error("Failed to set user's properties", e);
        }
        return null;
    }

}
