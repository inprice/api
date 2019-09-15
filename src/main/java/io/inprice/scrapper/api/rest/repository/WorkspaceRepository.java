package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.models.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class WorkspaceRepository {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    private static final BulkDeleteStatements bulkDeleteStatements = Beans.getSingleton(BulkDeleteStatements.class);

    public ServiceResponse<Workspace> findById(Long id) {
        Workspace model = dbUtils.findSingle(
            String.format(
            "select * from workspace " +
                "where id = %d " +
                "  and company_id = %d", id, Context.getCompanyId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        }
        return Responses.NotFound.WORKSPACE;
    }

    public ServiceResponse<Workspace> getList() {
        List<Workspace> workspaces = dbUtils.findMultiple(
            String.format(
            "select * from workspace " +
                "where company_id = %d " +
                "order by name", Context.getCompanyId()), this::map);

        if (workspaces != null && workspaces.size() > 0) {
            return new ServiceResponse<>(workspaces);
        }
        return Responses.NotFound.WORKSPACE;
    }

    /**
     * must be done by an admin
     *
     */
    public ServiceResponse insert(WorkspaceDTO workspaceDTO) {
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
            pst.setLong(++i, Context.getCompanyId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.DataProblem.DB_PROBLEM;

        } catch (SQLIntegrityConstraintViolationException ie) {
            log.error("Failed to insert workspace: " + ie.getMessage());
            return Responses.DataProblem.INTEGRITY_PROBLEM;
        } catch (Exception e) {
            log.error("Failed to insert workspace", e);
            return Responses.ServerProblem.EXCEPTION;
        }
    }

    /**
     * must be done by an admin
     *
     */
    public ServiceResponse update(WorkspaceDTO workspaceDTO) {
        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement("update workspace set name=? where id=? and company_id=?")) {

            int i = 0;
            pst.setString(++i, workspaceDTO.getName());
            pst.setLong(++i, workspaceDTO.getId());
            pst.setLong(++i, Context.getCompanyId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.NotFound.WORKSPACE;

        } catch (SQLException sqle) {
            log.error("Failed to update workspace", sqle);
            return Responses.ServerProblem.EXCEPTION;
        }
    }

    public ServiceResponse deleteById(Long id) {
        boolean result = dbUtils.executeBatchQueries(
            bulkDeleteStatements.workspaces(id),
            String.format("Failed to delete workspace. Id: %d", id), 1 //at least one execution must be successful
        );

        if (result) {
            return Responses.OK;
        }
        return Responses.NotFound.WORKSPACE;
    }

    private Workspace map(ResultSet rs) {
        try {
            Workspace model = new Workspace();
            model.setId(rs.getLong("id"));
            model.setActive(rs.getBoolean("active"));
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
