package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class LinkRepository {

    private static final Logger log = LoggerFactory.getLogger(LinkRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public ServiceResponse<Link> findById(AuthUser authUser, Long id) {
        Link model = dbUtils.findSingle(
            String.format(
            "select * from link " +
                "where id = %d " +
                "  and workspace_id = %d", id, authUser.getWorkspaceId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        } else {
            return InstantResponses.NOT_FOUND("Link");
        }
    }

    public ServiceResponse<Link> getList(AuthUser authUser, Long productId) {
        List<Link> links = dbUtils.findMultiple(
            String.format(
                "select * from link " +
                    "where product_id = %d " +
                    "  and workspace_id = %d " +
                    "order by name", productId, authUser.getWorkspaceId()), this::map);

        if (links != null && links.size() > 0) {
            return new ServiceResponse<>(links);
        }
        return InstantResponses.NOT_FOUND("Link");
    }

    public ServiceResponse insert(AuthUser authUser, LinkDTO linkDTO) {
        final String query =
                "insert into link " +
                "(url, product_id, workspace_id, company_id) " +
                "values " +
                "(?, ?, ?, ?) ";

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, linkDTO.getUrl());
            pst.setLong(++i, linkDTO.getProductId());
            pst.setLong(++i, authUser.getWorkspaceId());
            pst.setLong(++i, authUser.getCompanyId());

            if (pst.executeUpdate() > 0)
                return InstantResponses.OK;
            else
                return InstantResponses.CRUD_ERROR("Couldn't insert the link!");

        } catch (SQLIntegrityConstraintViolationException ie) {
            log.error("Failed to insert link: " + ie.getMessage());
            return InstantResponses.SERVER_ERROR(ie);
        } catch (Exception e) {
            log.error("Failed to insert link", e);
            return InstantResponses.SERVER_ERROR(e);
        }
    }

    public ServiceResponse deleteById(AuthUser authUser, Long id) {
        boolean result = dbUtils.executeBatchQueries(new String[] {

                String.format(
                "delete link_price where link_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete link_history where link_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete link_spec where link_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete link where id=%d and workspace_id=%d;",              //must be successful
                    id, authUser.getWorkspaceId()
                )

            }, String.format("Failed to delete link. Id: %d", id), 1 //1 of 4 execution must be successful

        );

        if (result) return InstantResponses.OK;

        return InstantResponses.NOT_FOUND("Link");
    }

    public ServiceResponse changeStatus(AuthUser authUser, Long id, Long productId, Status status) {
        ServiceResponse res;

        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            final String q1 =
                "update link " +
                "set previous_status=status, status=?, last_update=now() " +
                "where id=? " +
                "  and workspace_id=? " +
                "  and product_id=? " +
                "  and status != ? ";

            boolean res1;
            boolean res2 = false;

            try (PreparedStatement pst = con.prepareStatement(q1)) {
                int i = 0;
                pst.setString(++i, status.name());
                pst.setLong(++i, id);
                pst.setLong(++i, authUser.getWorkspaceId());
                pst.setLong(++i, productId);
                pst.setString(++i, status.name());

                res1 = (pst.executeUpdate() > 0);
            }

            if (res1) {
                try (PreparedStatement
                     pst = con.prepareStatement(
                         "insert into link_history (link_id, status, company_id, workspace_id, product_id) " +
                             "values (?, ?, ?, ?, ?)")) {
                    int i = 0;
                    pst.setLong(++i, id);
                    pst.setString(++i, status.name());
                    pst.setLong(++i, authUser.getCompanyId());
                    pst.setLong(++i, authUser.getWorkspaceId());
                    pst.setLong(++i, productId);

                    res2 = (pst.executeUpdate() > 0);
                }
            } else {
                log.warn("Link's status is already changed! Link Id: {}, New Status: {}", id, status);
            }

            if (res1) {
                if (res2) {
                    dbUtils.commit(con);
                    res = InstantResponses.OK;
                } else {
                    dbUtils.rollback(con);
                    res = InstantResponses.CRUD_ERROR("Seems that link's status is already changed!");
                }
            } else {
                res = InstantResponses.WRONG_PARAMETER("Invalid product!");
            }

        } catch (SQLException e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to change link's status. Link Id: " + id, e);
            res = InstantResponses.CRUD_ERROR(e.getMessage());
        } finally {
            if (con != null) dbUtils.close(con);
        }

        return res;
    }

    private Link map(ResultSet rs) {
        try {
            Link model = new Link();
            model.setId(rs.getLong("id"));
            model.setUrl(rs.getString("url"));
            model.setSku(rs.getString("sku"));
            model.setName(rs.getString("name"));
            model.setBrand(rs.getString("brand"));
            model.setSeller(rs.getString("seller"));
            model.setShipment(rs.getString("shipment"));
            model.setPrice(rs.getBigDecimal("price"));
            model.setStatus(Status.valueOf(rs.getString("status")));
            model.setPreviousStatus(Status.valueOf(rs.getString("previous_status")));
            model.setLastCheck(rs.getDate("last_check"));
            model.setLastUpdate(rs.getDate("last_update"));
            model.setRetry(rs.getInt("retry"));
            model.setHttpStatus(rs.getInt("http_status"));
            model.setWebsiteClassName(rs.getString("website_class_name"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));
            model.setProductId(rs.getLong("product_id"));
            model.setSiteId(rs.getLong("site_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set link's properties", e);
        }
        return null;
    }

}
