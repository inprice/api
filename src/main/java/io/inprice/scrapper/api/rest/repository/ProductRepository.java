package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.ImportProduct;
import io.inprice.scrapper.common.models.ImportProductRow;
import io.inprice.scrapper.common.models.Product;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
    private static final Properties properties = Beans.getSingleton(Properties.class);

    private static final BulkDeleteStatements bulkDeleteStatements = Beans.getSingleton(BulkDeleteStatements.class);

    public ServiceResponse<Product> findById(Long id) {
        Product model = dbUtils.findSingle(
            String.format(
            "select * from product " +
                "where id = %d " +
                "  and company_id = %d " +
                "  and workspace_id = %d ", id, Context.getCompanyId(), Context.getWorkspaceId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        } else {
            return InstantResponses.NOT_FOUND("Product");
        }
    }

    public ServiceResponse<Product> findByCode(String code) {
        Product model = dbUtils.findSingle(
            String.format(
            "select * from product " +
                "where code = '%s' " +
                "  and company_id = %d " +
                "  and workspace_id = %d ", code, Context.getCompanyId(), Context.getWorkspaceId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        } else {
            return InstantResponses.NOT_FOUND("Product");
        }
    }

    public ServiceResponse<Product> getList() {
        List<Product> products = dbUtils.findMultiple(
            String.format(
                "select * from product " +
                    "where company_id = %d " +
                    "  and workspace_id = %d " +
                    "order by name", Context.getCompanyId(), Context.getWorkspaceId()), this::map);

        if (products != null && products.size() > 0) {
            return new ServiceResponse<>(products);
        }
        return InstantResponses.NOT_FOUND("Product");
    }

    public ServiceResponse insert(ProductDTO productDTO) {
        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            if (properties.isProdUniqueness()) {
                boolean alreadyExists = doesExist(con, productDTO.getCode(), null);
                if (alreadyExists) {
                    return InstantResponses.ALREADY_EXISTS(productDTO.getCode());
                }
            }

            boolean result = insertANewProduct(con, productDTO);
            if (result) {
                dbUtils.commit(con);
                return InstantResponses.OK;
            } else {
                dbUtils.rollback(con);
                return InstantResponses.CRUD_ERROR("Couldn't insert the product. " + productDTO.toString());
            }

        } catch (Exception e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to insert a new product. " + productDTO, e);
            return InstantResponses.SERVER_ERROR(e);
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    public ServiceResponse update(ProductDTO productDTO) {
        Connection con = null;
        boolean result = false;

        try {
            con = dbUtils.getTransactionalConnection();

            if (properties.isProdUniqueness()) {
                boolean alreadyExists = doesExist(con, productDTO.getCode(), productDTO.getId());
                if (alreadyExists) {
                    return InstantResponses.ALREADY_EXISTS(productDTO.getCode());
                }
            }

            final String query =
                 "update product " +
                 "set code=?, name=?, brand=?, category=?, price=? " +
                 "where id=? " +
                 "  and company_id=? " +
                 "  and workspace_id=? ";

            try (PreparedStatement pst = con.prepareStatement(query)) {
                int i = 0;
                pst.setString(++i, productDTO.getCode());
                pst.setString(++i, productDTO.getName());
                pst.setString(++i, productDTO.getBrand());
                pst.setString(++i, productDTO.getCategory());
                pst.setBigDecimal(++i, productDTO.getPrice());
                pst.setLong(++i, productDTO.getId());
                pst.setLong(++i, Context.getCompanyId());
                pst.setLong(++i, Context.getWorkspaceId());

                if (pst.executeUpdate() > 0) {
                    result = addAPriceHistory(con, productDTO);
                }

                if (result) {
                    dbUtils.commit(con);
                    return InstantResponses.OK;
                } else {
                    dbUtils.rollback(con);
                    return InstantResponses.CRUD_ERROR("Couldn't update the product. " + productDTO.toString());
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

    public ServiceResponse deleteById(Long id) {
        boolean result = dbUtils.executeBatchQueries(
            bulkDeleteStatements.productsByProductId(id),
            String.format("Failed to delete product. Id: %d", id), 2 //at least two executions must be successful
        );

        if (result)
            return InstantResponses.OK;
        else
            return InstantResponses.NOT_FOUND("Product");
    }

    public ServiceResponse toggleStatus(Long id) {
        boolean result =
            dbUtils.executeQuery(
                String.format(
                "update product " +
                    "set active = not active " +
                    "where id = %d " +
                    "  and company_id = %d " +
                    "  and workspace_id = %d ", id, Context.getCompanyId(), Context.getWorkspaceId()),
        "Failed to toggle product status! id: " + id);

        if (result) return InstantResponses.OK;

        return InstantResponses.NOT_FOUND("Product");
    }

    private boolean addAPriceHistory(Connection con, ProductDTO productDTO) {
        final String query =
                "insert into product_price " +
                "(product_id, price, company_id, workspace_id) " +
                "values " +
                "(?, ?, ?, ?) ";

        try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setLong(++i, productDTO.getId());
            pst.setBigDecimal(++i, productDTO.getPrice());
            pst.setLong(++i, Context.getCompanyId());
            pst.setLong(++i, Context.getWorkspaceId());

            return (pst.executeUpdate() > 0);
        } catch (Exception e) {
            log.error("Error", e);
        }

        return false;
    }

    public ServiceResponse bulkInsert(ImportProduct report, List<ImportProductRow> importList) {
        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            final String headerQuery =
                "insert into import_product " +
                "(import_type, status, result, total_count, insert_count, duplicate_count, problem_count, company_id, workspace_id) " +
                "values " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?) ";

            Long importId = null;

            //these values may change when duplicated codes occur
            int insertCount = report.getInsertCount();
            int duplicateCount = report.getDuplicateCount();

            try (PreparedStatement pst = con.prepareStatement(headerQuery, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                pst.setString(++i, report.getImportType().name());
                pst.setInt(++i, report.getStatus());
                pst.setString(++i, report.getResult());
                pst.setInt(++i, report.getTotalCount());
                pst.setInt(++i, report.getInsertCount());
                pst.setInt(++i, report.getDuplicateCount());
                pst.setInt(++i, report.getProblemCount());
                pst.setLong(++i, Context.getCompanyId());
                pst.setLong(++i, Context.getWorkspaceId());

                if (pst.executeUpdate() > 0) {
                    try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            importId = generatedKeys.getLong(1);
                        }
                    }
                }
            }

            if (importId != null) {
                final String rowQuery =
                    "insert into import_product_row " +
                    "(import_id, import_type, data, status, description, company_id, workspace_id) " +
                    "values " +
                    "(?, ?, ?, ?, ?, ?, ?) ";
                try (PreparedStatement pst = con.prepareStatement(rowQuery)) {
                    for (ImportProductRow importRow: importList) {

                        ProductDTO dto = null;
                        boolean found = false;

                        if (importRow.getProductDTO() != null) {
                            dto = (ProductDTO) importRow.getProductDTO();
                            found = doesExist(con, dto.getCode(), null);
                        }

                        if (found) {
                            importRow.setDescription("Already exists!");
                            importRow.setStatus(Status.DUPLICATE);
                            report.incDuplicateCount();
                            report.decInsertCount();
                        } else if (dto != null) {
                            insertANewProduct(con, dto);
                        }

                        int i = 0;
                        pst.setLong(++i, importId);
                        pst.setString(++i, importRow.getImportType().name());
                        pst.setString(++i, importRow.getData());
                        pst.setString(++i, importRow.getStatus().name());
                        pst.setString(++i, importRow.getDescription());
                        pst.setLong(++i, Context.getCompanyId());
                        pst.setLong(++i, Context.getWorkspaceId());
                        pst.executeUpdate();
                    }
                }

                //insertCount and duplicateCount may change if duplicate codes found,
                // so we need to update the report data with the most accurate values
                if (insertCount != report.getInsertCount() || duplicateCount != report.getDuplicateCount()) {
                    final String lastUpdateQuery =
                        "update import_product " +
                        "set insert_count=?, duplicate_count=? " +
                        "where id=? ";
                    try (PreparedStatement pst = con.prepareStatement(lastUpdateQuery)) {
                        int i = 0;
                        pst.setInt(++i, insertCount);
                        pst.setInt(++i, duplicateCount);
                        pst.setLong(++i, importId);
                        pst.executeUpdate();
                    }
                }

                dbUtils.commit(con);
                return InstantResponses.OK;
            } else {
                dbUtils.rollback(con);
                return new ServiceResponse(HttpStatus.NOT_IMPLEMENTED_501, "Import operations failed!");
            }

        } catch (Exception e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to import new products. ", e);
            return InstantResponses.SERVER_ERROR(e);
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    private boolean doesExist(Connection con, String code, Long id) {
        final String query =
            "select id from product " +
            "where code=? " +
            (id != null ? " and id != " + id : "") +
            "  and company_id=? " +
            "  and workspace_id=? ";

        try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setString(++i, code);
            pst.setLong(++i, Context.getCompanyId());
            pst.setLong(++i, Context.getWorkspaceId());

            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (Exception e) {
            log.error("Error", e);
        }
        return false;
    }

    public int findProductCount() {
        int result = 0;

        try (Connection con = dbUtils.getConnection()) {

            //from product definition
            final String q1 =
                "select count(id) from product " +
                "where company_id=? " +
                "  and workspace_id=? ";
            try (PreparedStatement pst = con.prepareStatement(q1)) {
                pst.setLong(1, Context.getCompanyId());
                pst.setLong(2, Context.getWorkspaceId());

                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    result += rs.getInt(1);
                }
                rs.close();
            }

            //from product imports
            final String q2 =
                "select count(id) from import_product_row " +
                "where status=? " +
                "  and company_id=? " +
                "  and workspace_id=? ";
            try (PreparedStatement pst = con.prepareStatement(q2)) {
                pst.setString(1, Status.NEW.name());
                pst.setLong(2, Context.getCompanyId());
                pst.setLong(3, Context.getWorkspaceId());

                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    result += rs.getInt(1);
                }
                rs.close();
            }

        } catch (Exception e) {
            log.error("Error", e);
        }
        return result;
    }

    private boolean insertANewProduct(Connection con, ProductDTO productDTO) throws SQLException {
        final String query =
            "insert into product " +
            "(code, name, brand, category, price, company_id, workspace_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?, ?) ";

        try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setString(++i, productDTO.getCode());
            pst.setString(++i, productDTO.getName());
            pst.setString(++i, productDTO.getBrand());
            pst.setString(++i, productDTO.getCode());
            pst.setBigDecimal(++i, productDTO.getPrice());
            pst.setLong(++i, Context.getCompanyId());
            pst.setLong(++i, Context.getWorkspaceId());

            if (pst.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        productDTO.setId(generatedKeys.getLong(1));
                        return addAPriceHistory(con, productDTO);
                    }
                }
            }
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
