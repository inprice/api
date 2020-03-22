package io.inprice.scrapper.api.app.product_import;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class ProductImportRepository {

   private static final Logger log = LoggerFactory.getLogger(ProductImportRepository.class);
   private static final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findById(Long id) {
      ImportProduct model = db.findSingle(String.format(
            "select * from import_product where id = %d and company_id = %d ", id, CurrentUser.getCompanyId()), this::map);

      if (model != null) {
         // finding rows
         List<ImportProductRow> rowList = db
               .findMultiple(String.format("select * from import_product_row where import_id = %d and company_id = %d ",
                     id, CurrentUser.getCompanyId()), this::rowMap);
         model.setRowList(rowList);
         return new ServiceResponse(model);
      }

      return Responses.NotFound.IMPORT;
   }

   public ServiceResponse getList() {
      List<ImportProduct> imports = db.findMultiple(
            String.format("select * from import_product where company_id = %d order by created_at desc, import_type",
                  CurrentUser.getCompanyId()),
            this::map);

      return new ServiceResponse(imports);
   }

   public ServiceResponse deleteById(Long id) {
      final String subQuery = "(select id from link where import_id=%d and company_id=%d)";

      boolean result = db.executeBatchQueries(new String[] {
            String.format("delete from link_price where link_id in " + subQuery, id, CurrentUser.getCompanyId()),

            String.format("delete from link_spec where link_id in " + subQuery, id, CurrentUser.getCompanyId()),

            String.format("delete from link_history where link_id in " + subQuery, id, CurrentUser.getCompanyId()),

            String.format("delete from link where import_id=%d and company_id=%d ", id, CurrentUser.getCompanyId()),

            String.format("delete from import_product_row where import_id=%d and company_id=%d ", id,
                  CurrentUser.getCompanyId()),

            String.format("delete from import_product where id=%d and company_id=%d ", id, CurrentUser.getCompanyId()) },

            String.format("Failed to delete import. Id: %d", id), 1 // at least one execution must be successful
      );

      if (result)
         return Responses.OK;
      else
         return Responses.NotFound.IMPORT;
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

         return model;
      } catch (SQLException e) {
         log.error("Failed to set import's properties", e);
      }
      return null;
   }

   private ImportProductRow rowMap(ResultSet rs) {
      try {
         ImportProductRow model = new ImportProductRow();
         model.setId(rs.getLong("id"));
         model.setImportId(rs.getLong("import_id"));
         model.setImportType(ImportType.valueOf(rs.getString("import_type")));
         model.setData(rs.getString("data"));
         model.setStatus(LinkStatus.valueOf(rs.getString("status")));
         model.setLastUpdate(rs.getDate("last_update"));
         model.setDescription(rs.getString("description"));
         model.setLinkId(rs.getLong("link_id"));
         model.setCompanyId(rs.getLong("company_id"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set import product row's properties", e);
      }
      return null;
   }

}
