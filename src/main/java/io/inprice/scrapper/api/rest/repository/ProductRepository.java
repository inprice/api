package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.models.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public ServiceResponse<Product> findById(AuthUser authUser, Long id) {
        Product model = dbUtils.findSingle(
            String.format(
            "select * from product " +
                "where id = %d " +
                "  and workspace_id = %d", id, authUser.getWorkspaceId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        } else {
            return InstantResponses.NOT_FOUND("Product");
        }
    }

    public ServiceResponse<Product> getList(AuthUser authUser) {
        List<Product> products = dbUtils.findMultiple(
            String.format(
                "select * from product " +
                    "where workspace_id = %d " +
                    "order by name", authUser.getWorkspaceId()), this::map);

        if (products != null && products.size() > 0) {
            return new ServiceResponse<>(products);
        }
        return InstantResponses.NOT_FOUND("Product");
    }

    public ServiceResponse insert(AuthUser authUser, ProductDTO productDTO) {
        Connection con = null;
        boolean result = false;

        try {
            con = dbUtils.getTransactionalConnection();

            final String query =
                    "insert into product " +
                    "(code, name, brand, category, price, workspace_id, company_id) " +
                    "values " +
                    "(?, ?, ?, ?, ?, ?, ?) ";

            try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                pst.setString(++i, productDTO.getCode());
                pst.setString(++i, productDTO.getName());
                pst.setString(++i, productDTO.getBrand());
                pst.setString(++i, productDTO.getCode());
                pst.setBigDecimal(++i, productDTO.getPrice());
                pst.setLong(++i, authUser.getWorkspaceId());
                pst.setLong(++i, authUser.getCompanyId());

                if (pst.executeUpdate() > 0) {
                    try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            productDTO.setId(generatedKeys.getLong(1));
                            result = addAPriceHistory(authUser, con, productDTO);
                        }
                    }
                }
            }

            if (result) {
                dbUtils.commit(con);
                return InstantResponses.OK;
            } else {
                dbUtils.rollback(con);
                return InstantResponses.CRUD_ERROR("");
            }

        } catch (Exception e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to insert a new product. " + productDTO, e);
            return InstantResponses.SERVER_ERROR(e);
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    public ServiceResponse update(AuthUser authUser, ProductDTO productDTO) {
        Connection con = null;
        boolean result = false;

        try {
            con = dbUtils.getTransactionalConnection();

            final String query =
                 "update product " +
                 "set code=?, name=?, brand=?, category=?, price=? " +
                 "where id=? " +
                 "  and workspace_id=?";

            try (PreparedStatement pst = con.prepareStatement(query)) {
                int i = 0;
                pst.setString(++i, productDTO.getCode());
                pst.setString(++i, productDTO.getName());
                pst.setString(++i, productDTO.getBrand());
                pst.setString(++i, productDTO.getCategory());
                pst.setBigDecimal(++i, productDTO.getPrice());
                pst.setLong(++i, productDTO.getId());
                pst.setLong(++i, authUser.getWorkspaceId());

                if (pst.executeUpdate() > 0) {
                    result = addAPriceHistory(authUser, con, productDTO);
                }

                if (result) {
                    dbUtils.commit(con);
                    return InstantResponses.OK;
                } else {
                    dbUtils.rollback(con);
                    return InstantResponses.CRUD_ERROR("");
                }
            }
        } catch (Exception e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to update a product. " + productDTO, e);
            return InstantResponses.SERVER_ERROR(e);
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    public ServiceResponse deleteById(AuthUser authUser, Long id) {
        boolean result = dbUtils.executeBatchQueries(new String[] {

                String.format(
                "delete link_price where product_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete link_history where product_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete link_spec where product_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete link where product_id=%d and workspace_id=%d;",
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete product_price where product_id=%d and workspace_id=%d;",  //must be successful
                    id, authUser.getWorkspaceId()
                ),
                String.format(
                "delete product where id=%d and workspace_id=%d;",                //must be successful
                    id, authUser.getWorkspaceId()
                )

            }, String.format("Failed to delete product. Id: %d", id), 2 //2 of 6 execution must be successful

        );

        if (result) return InstantResponses.OK;

        return InstantResponses.NOT_FOUND("Product");
    }

    public ServiceResponse toggleStatus(AuthUser authUser, Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                "update product " +
                    "set active = not active " +
                    "where id = %d " +
                    "  and workspace_id = %d ", id, authUser.getWorkspaceId()),
        "Failed to toggle product status! id: " + id);

        if (result) return InstantResponses.OK;

        return InstantResponses.NOT_FOUND("Product");
    }

    private boolean addAPriceHistory(AuthUser authUser, Connection con, ProductDTO productDTO) {
        final String query =
                "insert into product_price " +
                "(product_id, price, workspace_id, company_id) " +
                "values " +
                "(?, ?, ?, ?) ";

        try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setLong(++i, productDTO.getId());
            pst.setBigDecimal(++i, productDTO.getPrice());
            pst.setLong(++i, authUser.getWorkspaceId());
            pst.setLong(++i, authUser.getCompanyId());

            return (pst.executeUpdate() > 0);
        } catch (Exception e) {
            log.error("Error", e);
        }

        return false;
    }

    private Product map(ResultSet rs) {
        try {
            Product model = new Product();
            model.setId(rs.getLong("id"));
            model.setActive(rs.getBoolean("active"));
            model.setCode(rs.getString("code"));
            model.setName(rs.getString("name"));
            model.setBrand(rs.getString("brand"));
            model.setCategory(rs.getString("category"));
            model.setPrice(rs.getBigDecimal("price"));
            model.setPosition(rs.getInt("position"));
            model.setMinSeller(rs.getString("min_seller"));
            model.setMaxSeller(rs.getString("max_seller"));
            model.setMinPrice(rs.getBigDecimal("min_price"));
            model.setAvgPrice(rs.getBigDecimal("avg_price"));
            model.setMaxPrice(rs.getBigDecimal("max_price"));
            model.setLastUpdate(rs.getDate("last_update"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set product's properties", e);
        }
        return null;
    }

}
