package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.common.models.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class WorkspaceRepository {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public ServiceResponse<Workspace> findById(AuthUser claims, Long id) {
        Workspace model = dbUtils.findSingle(
            String.format(
            "select * from workspace " +
                "where id = %d " +
                "  and company_id = %d", id, claims.getCompanyId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        } else {
            return InstantResponses.NOT_FOUND("Workspace");
        }
    }

    public ServiceResponse<Workspace> getList(long companyId) {
        List<Workspace> workspaces = dbUtils.findMultiple(
            String.format(
                "select * from workspace " +
                    "where company_id = %d " +
                    "order by name", companyId), this::map);

        if (workspaces != null && workspaces.size() > 0) {
            return new ServiceResponse<>(workspaces);
        }
        return InstantResponses.NOT_FOUND("Workspace");
    }

    /**
     * must be done by an admin
     *
     */
    public ServiceResponse<Workspace> insert(Long companyId, WorkspaceDTO workspaceDTO) {
        final String query =
                "insert into workspace " +
                "(name, plan_id, company_id) " +
                "values " +
                "(?, ?, ?) ";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, workspaceDTO.getName());
            pst.setLong(++i, workspaceDTO.getPlanId());
            pst.setLong(++i, companyId);

            if (pst.executeUpdate() > 0)
                return InstantResponses.OK;
            else
                return InstantResponses.CRUD_ERROR;

        } catch (SQLIntegrityConstraintViolationException ie) {
            log.error("Failed to insert workspace: " + ie.getMessage());
            return InstantResponses.SERVER_ERROR;
        } catch (Exception e) {
            log.error("Failed to insert workspace", e);
            return InstantResponses.SERVER_ERROR;
        }
    }

    /**
     * must be done by an admin
     *
     */
    public ServiceResponse<Workspace> update(WorkspaceDTO workspaceDTO) {
        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement("update workspace set name=? where id=?")) {

            int i = 0;
            pst.setString(++i, workspaceDTO.getName());
            pst.setLong(++i, workspaceDTO.getId());

            if (pst.executeUpdate() > 0)
                return InstantResponses.OK;
            else
                return InstantResponses.NOT_FOUND("Workspace");

        } catch (SQLException sqle) {
            log.error("Failed to update workspace", sqle);
            return InstantResponses.SERVER_ERROR;
        } catch (Exception e) {
            log.error("Failed to update workspace", e);
            return InstantResponses.SERVER_ERROR;
        }
    }

    /**
     * must be done by an admin
     *
     */
    public ServiceResponse<Workspace> deleteById(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format("delete from workspace where id = %d ", id),"Failed to delete workspace with id: " + id);

        if (result) return InstantResponses.OK;

        return InstantResponses.NOT_FOUND("Workspace");
    }

    private Workspace map(ResultSet rs) {
        try {
            Workspace model = new Workspace();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setDueDate(rs.getDate("due_date"));
            model.setLastCollectingTime(rs.getDate("last_collecting_time"));
            model.setLastCollectingStatus(rs.getBoolean("last_collecting_status"));
            model.setRetry(rs.getInt("retry"));
            model.setPlanId(rs.getLong("plan_id"));
            model.setCompanyId(rs.getLong("company_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set workspace's properties", e);
        }
        return null;
    }

}
