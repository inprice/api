package io.inprice.scrapper.api.app.product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.app.product_import.ImportProduct;
import io.inprice.scrapper.api.app.product_import.ImportProductRow;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.BulkDeleteStatements;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.helpers.SqlHelper;

public class ProductRepository {

   private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

   private static final Database db = Beans.getSingleton(Database.class);
   private static final BulkDeleteStatements bulkDeleteStatements = Beans.getSingleton(BulkDeleteStatements.class);

   public ServiceResponse findById(Long id) {
      Product model = db.findSingle(
            String.format("select * from product where id = %d and company_id = %d ", id, CurrentUser.getCompanyId()),
            this::map);
      if (model != null) {
         return new ServiceResponse(model);
      }
      return Responses.NotFound.PRODUCT;
   }

   public ServiceResponse findByCode(String code) {
      Product model = db.findSingle(String.format("select * from product where code = '%s' and company_id = %d ",
            SqlHelper.clear(code), CurrentUser.getCompanyId()), this::map);
      if (model != null) {
         return new ServiceResponse(model);
      }
      return Responses.NotFound.PRODUCT;
   }

   public ServiceResponse getList() {
      List<Product> products = db.findMultiple(
            String.format("select * from product where company_id = %d order by name", CurrentUser.getCompanyId()),
            this::map);

      return new ServiceResponse(products);
   }

   public ServiceResponse search(SearchModel searchModel) {
      final String searchQueryForRowCount = SqlHelper.generateSearchQueryCountPart("product", searchModel, "code",
            "name");

      int totalRowCount = 0;
      try (Connection con = db.getConnection();
            PreparedStatement pst = con.prepareStatement(searchQueryForRowCount)) {

         ResultSet rs = pst.executeQuery();
         if (rs.next()) {
            totalRowCount = rs.getInt(1);
         }
         rs.close();

         if (totalRowCount > 0) {
            final String searchQueryForSelection = SqlHelper.generateSearchQuerySelectPart("product", searchModel,
                  totalRowCount, "code", "name");
            List<Product> rows = db.findMultiple(con, searchQueryForSelection, this::map);
            Map<String, Object> data = new HashMap<>(4);
            data.put("rows", rows);
            data.put("totalRowCount", totalRowCount);
            data.put("totalPageCount", 1);
            if (totalRowCount > searchModel.getPageLimit()) {
               data.put("totalPageCount", Math.ceil((double) totalRowCount / searchModel.getPageLimit()));
            }
            return new ServiceResponse(data);
         }

      } catch (Exception e) {
         log.error("Failed to search products. ", e);
         return Responses.ServerProblem.EXCEPTION;
      }

      return Responses.NotFound.SEARCH_NOT_FOUND;
   }

   public ServiceResponse insert(ProductDTO dto) {
      Connection con = null;
      try {
         con = db.getTransactionalConnection();

         if (Props.isProdUniqueness()) {
            boolean alreadyExists = doesExist(con, dto.getCode(), null);
            if (alreadyExists) {
               return Responses.DataProblem.ALREADY_EXISTS;
            }
         }

         ServiceResponse result = insertANewProduct(con, dto);
         if (result.isOK()) {
            db.commit(con);
            return Responses.OK;
         } else {
            db.rollback(con);
            return result;
         }

      } catch (Exception e) {
         if (con != null)
            db.rollback(con);
         log.error("Failed to insert a new product. " + dto, e);
         return Responses.ServerProblem.EXCEPTION;
      } finally {
         if (con != null)
            db.close(con);
      }
   }

   public ServiceResponse update(ProductDTO dto) {
      Connection con = null;
      boolean result = false;

      try {
         con = db.getTransactionalConnection();

         if (Props.isProdUniqueness()) {
            boolean alreadyExists = doesExist(con, dto.getCode(), dto.getId());
            if (alreadyExists) {
               return Responses.DataProblem.ALREADY_EXISTS;
            }
         }

         final String query = "update product set code=?, name=?, brand=?, category=?, price=? where id=? and company_id=? ";

         try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setString(++i, dto.getCode());
            pst.setString(++i, dto.getName());
            pst.setString(++i, dto.getBrand());
            pst.setString(++i, dto.getCategory());
            pst.setBigDecimal(++i, dto.getPrice());
            pst.setLong(++i, dto.getId());
            pst.setLong(++i, CurrentUser.getCompanyId());

            if (pst.executeUpdate() > 0) {
               result = addAPriceHistory(con, dto);
            }

            if (result) {
               db.commit(con);
               return Responses.OK;
            } else {
               db.rollback(con);
               return Responses.DataProblem.DB_PROBLEM;
            }
         }
      } catch (SQLIntegrityConstraintViolationException duperr) {
         if (con != null)
            db.rollback(con);
         log.error("Code duplication error " + dto.getCode(), duperr.getMessage());
         return new ServiceResponse(Responses.DataProblem.DUPLICATE.getStatus(),
               dto.getCode() + " is already used for another product!");
      } catch (Exception e) {
         if (con != null)
            db.rollback(con);
         log.error("Failed to update a product. " + dto, e);
         return Responses.ServerProblem.EXCEPTION;
      } finally {
         if (con != null)
            db.close(con);
      }
   }

   public ServiceResponse deleteById(Long id) {
      boolean result = db.executeBatchQueries(bulkDeleteStatements.products(id),
            String.format("Failed to delete product. Id: %d", id), 2 // at least two executions must be successful
      );

      if (result)
         return Responses.OK;
      else
         return Responses.NotFound.PRODUCT;
   }

   public ServiceResponse toggleStatus(Long id) {
      boolean result = db
            .executeQuery(String.format("update product set active = not active where id = %d and company_id = %d ", id,
                  CurrentUser.getCompanyId()), "Failed to toggle product status! id: " + id);

      if (result) {
         return Responses.OK;
      }
      return Responses.NotFound.PRODUCT;
   }

   private boolean addAPriceHistory(Connection con, ProductDTO dto) {
      final String query = "insert into product_price (product_id, price, company_id) values (?, ?, ?) ";

      try (PreparedStatement pst = con.prepareStatement(query)) {
         int i = 0;
         pst.setLong(++i, dto.getId());
         pst.setBigDecimal(++i, dto.getPrice());
         pst.setLong(++i, CurrentUser.getCompanyId());

         return (pst.executeUpdate() > 0);
      } catch (Exception e) {
         log.error("Error", e);
      }

      return false;
   }

   public ServiceResponse bulkInsert(ImportProduct report, List<ImportProductRow> importList) {
      Connection con = null;
      try {
         con = db.getTransactionalConnection();

         final String headerQuery = "insert into import_product "
               + "(import_type, status, result, total_count, insert_count, duplicate_count, problem_count, company_id) "
               + "values " + "(?, ?, ?, ?, ?, ?, ?, ?) ";

         Long importId = null;

         // these values may change when duplicated codes occur
         final int insertCount = report.getInsertCount();
         final int duplicateCount = report.getDuplicateCount();

         try (PreparedStatement pst = con.prepareStatement(headerQuery, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setString(++i, report.getImportType().name());
            pst.setInt(++i, report.getStatus());
            pst.setString(++i, report.getResult());
            pst.setInt(++i, report.getTotalCount());
            pst.setInt(++i, report.getInsertCount());
            pst.setInt(++i, report.getDuplicateCount());
            pst.setInt(++i, report.getProblemCount());
            pst.setLong(++i, CurrentUser.getCompanyId());

            if (pst.executeUpdate() > 0) {
               try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                  if (generatedKeys.next()) {
                     importId = generatedKeys.getLong(1);
                  }
               }
            }
         }

         if (importId != null) {
            final String rowQuery = "insert into import_product_row "
                  + "(import_id, import_type, data, status, last_update, description, link_id, company_id) " + "values "
                  + "(?, ?, ?, ?, ?, ?, ?, ?) ";
            try (PreparedStatement pst = con.prepareStatement(rowQuery, Statement.RETURN_GENERATED_KEYS)) {
               for (ImportProductRow importRow : importList) {

                  ProductDTO dto = null;
                  boolean found = false;
                  Long linkId = null;

                  if (importRow.getProductDTO() != null) {
                     dto = (ProductDTO) importRow.getProductDTO();
                     found = doesExist(con, dto.getCode(), null);
                  }

                  importRow.setImportId(importId);
                  importRow.setLastUpdate(new Date());

                  /*
                   * Please note that: In case of importing CSV files, no need to insert any row
                   * in to link table. Instead, we add a new row only to product table. We need to
                   * track links by their importRowId in order to manage them appropriately.
                   */
                  if (found) {
                     importRow.setDescription("Already exists!");
                     importRow.setStatus(LinkStatus.DUPLICATE);
                     report.setDuplicateCount(report.getDuplicateCount() + 1);
                     report.setInsertCount(report.getInsertCount() + 1);
                  } else if (dto != null) { // if is a CSV import, no need to insert any row into link table
                     dto.setImportId(importId);
                     insertANewProduct(con, dto);
                  } else { // if not a CSV import, no need to insert any row into product table (this will
                           // automatically be done later)
                     linkId = insertImportedLink(con, importRow);
                  }

                  int i = 0;
                  pst.setLong(++i, importRow.getImportId());
                  pst.setString(++i, report.getImportType().name());
                  pst.setString(++i, importRow.getData());
                  pst.setString(++i, importRow.getStatus().name());
                  pst.setDate(++i, new java.sql.Date(importRow.getLastUpdate().getTime()));
                  pst.setString(++i, importRow.getDescription());
                  if (linkId != null)
                     pst.setLong(++i, linkId);
                  else
                     pst.setNull(++i, Types.BIGINT);
                  pst.setLong(++i, CurrentUser.getCompanyId());

                  int affected = pst.executeUpdate();

                  if (affected > 0 && linkId != null) { // which means not a CSV import, so link table's importRowId
                                                        // field is set
                     try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                           long importRowId = generatedKeys.getLong(1);

                           try (PreparedStatement pst1 = con
                                 .prepareStatement("update link set import_row_id=? where id=? and company_id=?")) {
                              int j = 0;
                              pst1.setLong(++j, importRowId);
                              pst1.setLong(++j, linkId);
                              pst1.setLong(++j, CurrentUser.getCompanyId());

                              int affected1 = pst1.executeUpdate();
                              if (affected1 < 1) {
                                 log.warn("Setting import_row_id field in link table failed! Link id: {}", linkId);
                              }
                           }
                        }
                     }
                  }
               }
            }

            // insertCount and duplicateCount may change if duplicate codes found,
            // so we need to update the report data with the most accurate values
            if (insertCount != report.getInsertCount() || duplicateCount != report.getDuplicateCount()) {
               final String lastUpdateQuery = "update import_product " + "set insert_count=?, duplicate_count=? "
                     + "where id=? ";
               try (PreparedStatement pst = con.prepareStatement(lastUpdateQuery)) {
                  int i = 0;
                  pst.setInt(++i, report.getInsertCount());
                  pst.setInt(++i, report.getDuplicateCount());
                  pst.setLong(++i, importId);
                  pst.executeUpdate();
               }
            }

            db.commit(con);
            return Responses.OK;
         } else {
            db.rollback(con);
            return Responses.ServerProblem.FAILED;
         }

      } catch (Exception e) {
         if (con != null)
            db.rollback(con);
         log.error("Failed to import new products. ", e);
         return Responses.ServerProblem.EXCEPTION;
      } finally {
         if (con != null)
            db.close(con);
      }
   }

   private boolean doesExist(Connection con, String code, Long id) {
      final String query = "select id from product where code=? " + (id != null ? " and id != " + id : "")
            + "  and company_id=? ";

      try (PreparedStatement pst = con.prepareStatement(query)) {
         int i = 0;
         pst.setString(++i, code);
         pst.setLong(++i, CurrentUser.getCompanyId());

         ResultSet rs = pst.executeQuery();
         return rs.next();
      } catch (Exception e) {
         log.error("Error", e);
      }
      return false;
   }

   public int findProductCount() {
      int result = 0;

      try (Connection con = db.getConnection()) {

         // from product definition
         try (PreparedStatement pst = con.prepareStatement("select count(id) from product where company_id=? ")) {
            pst.setLong(1, CurrentUser.getCompanyId());

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
               result += rs.getInt(1);
            }
            rs.close();
         }

         // from product imports
         try (PreparedStatement pst = con
               .prepareStatement("select count(id) from import_product_row where status=? and company_id=?")) {
            pst.setString(1, LinkStatus.NEW.name());
            pst.setLong(2, CurrentUser.getCompanyId());

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

   @SuppressWarnings("incomplete-switch")
   private Long insertImportedLink(Connection con, ImportProductRow importRow) {

      final String query = "insert into link (url, import_id, company_id) values (?, ?, ?)";
      try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
         int i = 0;

         switch (importRow.getImportType()) {
            case URL: {
               pst.setString(++i, importRow.getData());
               break;
            }
            case EBAY_SKU: {
               pst.setString(++i, Props.getPrefix_ForSearchingInEbay() + importRow.getData());
               break;
            }
            case AMAZON_ASIN: {
               pst.setString(++i, Props.getPrefix_ForSearchingInAmazon() + importRow.getData());
               break;
            }
         }

         pst.setLong(++i, importRow.getImportId());
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
               if (generatedKeys.next()) {
                  return generatedKeys.getLong(1);
               }
            }
         }

      } catch (SQLException e) {
         e.printStackTrace();
      }

      return null;
   }

   private ServiceResponse insertANewProduct(Connection con, ProductDTO dto) {

      final String query = "insert into product "
            + "(code, name, brand, category, price, import_id, company_id) values (?, ?, ?, ?, ?, ?)";
      try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
         int i = 0;
         pst.setString(++i, dto.getCode());
         pst.setString(++i, dto.getName());
         pst.setString(++i, dto.getBrand());
         pst.setString(++i, dto.getCode());
         pst.setBigDecimal(++i, dto.getPrice());
         if (dto.getImportId() != null)
            pst.setLong(++i, dto.getImportId());
         else
            pst.setNull(++i, Types.BIGINT);
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
               if (generatedKeys.next()) {
                  dto.setId(generatedKeys.getLong(1));
                  boolean ok = addAPriceHistory(con, dto);
                  if (ok) {
                     return Responses.OK;
                  } else {
                     return Responses.DataProblem.DB_PROBLEM;
                  }
               }
            }
         }
      } catch (SQLIntegrityConstraintViolationException ie) {
         return new ServiceResponse(Responses.DataProblem.DUPLICATE.getStatus(),
               dto.getCode() + " is already defined!");
      } catch (Exception e) {
         log.error("Error", e);
      }

      return Responses.DataProblem.DB_PROBLEM;
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
         model.setCompanyId(rs.getLong("company_id"));
         model.setUpdatedAt(rs.getDate("updated_at"));
         model.setCreatedAt(rs.getDate("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set product's properties", e);
      }
      return null;
   }

}
