package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.models.ImportProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ProductImportRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductImportRepository.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public ServiceResponse<ImportProduct> findById(Long id) {
        ImportProduct model = dbUtils.findSingle(
            String.format(
            "select * from import_product " +
                "where id = %d " +
                "  and company_id = %d " +
                "  and workspace_id = %d ", id, Context.getCompanyId(), Context.getWorkspaceId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        } else {
            return InstantResponses.NOT_FOUND("Product");
        }
    }

    public ServiceResponse<ImportProduct> getList() {
        List<ImportProduct> imports = dbUtils.findMultiple(
            String.format(
                "select * from import_product " +
                    "where company_id = %d " +
                    "  and workspace_id = %d " +
                    "order by insert_at desc, import_type", Context.getCompanyId(), Context.getWorkspaceId()), this::map);

        if (imports != null && imports.size() > 0) {
            return new ServiceResponse<>(imports);
        }
        return InstantResponses.NOT_FOUND("Import");
    }

    public ServiceResponse deleteById(Long id) {
        boolean result = dbUtils.executeBatchQueries(new String[]{
            String.format(
            "delete from import_product_row where import_id=%d and company_id=%d and workspace_id=%d ",
                id, Context.getCompanyId(), Context.getWorkspaceId()
            ),

            String.format(
            "delete from import_product where id=%d and company_id=%d and workspace_id=%d ",
                id, Context.getCompanyId(), Context.getWorkspaceId()
            )},

            String.format("Failed to delete import. Id: %d", id), 1 //at least one execution must be successful
        );

        if (result)
            return InstantResponses.OK;
        else
            return InstantResponses.NOT_FOUND("Import");
    }

    private ImportProduct map(ResultSet rs) {
        try {
            ImportProduct model = new ImportProduct();
            model.setId(rs.getLong("id"));
            model.setImportType(ImportType.valueOf(rs.getString("import_type")));
            model.setStatus(rs.getInt("status"));
            model.setResult(rs.getString("result"));
            model.setTotalCount(rs.getInt("total_count"));
            model.setInsertCount(rs.getInt("insert_count"));
            model.setDuplicateCount(rs.getInt("duplicate_count"));
            model.setProblemCount(rs.getInt("problem_count"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set import's properties", e);
        }
        return null;
    }

}
