package io.inprice.scrapper.api.app.product_import;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class ProductImportRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductImportRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

  public ServiceResponse findById(Long id) {
    ImportProduct model = db.findSingle(String.format("select * from import_product where id = %d and company_id = %d ",
        id, CurrentUser.getCompanyId()), this::map);

    if (model != null) {
      return new ServiceResponse(model);
    }

    return Responses.NotFound.IMPORT;
  }

  public ServiceResponse getList() {
    return new ServiceResponse(
      db.findMultiple(String.format("select * from import_product where company_id = %d order by status, import_type",
          CurrentUser.getCompanyId()), this::map)
    );
  }

  public ServiceResponse deleteById(Long id) {
    boolean result =
      db.executeQuery(
        String.format("delete from import_product where id=%d and company_id=%d ", id, CurrentUser.getCompanyId()),
        String.format("Failed to delete link. Id: %d", id));
    if (result)
      return Responses.OK;
    else
      return Responses.NotFound.IMPORT;
  }

  public ServiceResponse bulkInsert(Collection<ImportProduct> imports) {
    final String query = "insert into import_product (import_type, status, data, description, company_id) values ('%s', '%s', '%s', '%s', %d); ";

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      List<String> queries = new ArrayList<>(imports.size());
      Iterator<ImportProduct> itr = imports.iterator();
      while (itr.hasNext()) {
        ImportProduct row = itr.next();
        String data = null;

        switch (row.getImportType()) {
          case EBAY_SKU: {
            data = Props.getPrefix_ForSearchingInEbay() + row.getData();
            break;
          }
          case AMAZON_ASIN: {
            data = Props.getPrefix_ForSearchingInAmazon() + row.getData();
            break;
          }
          default:
            data = row.getData();
            break;
        }

        // adding product if any
        if (row.getStatus().equals(LinkStatus.NEW) && row.getProductDTO() != null) {
          ServiceResponse res = productRepository.insertANewProduct(con, row.getProductDTO());
          if (! res.isOK()) {
            if (res.equals(Responses.DataProblem.DUPLICATE)) {
              row.setStatus(LinkStatus.DUPLICATE);
            }
            row.setDescription(res.getReason());
          }
        }

        // adding import row
        queries.add( 
          String.format(query,
            row.getImportType().name(),
            row.getStatus().name(),
            SqlHelper.clear(data),
            SqlHelper.clear(row.getDescription()),
            CurrentUser.getCompanyId()
          )
        );

      }

      boolean result = db.executeBatchQueries(con, queries, "Failed to import products");

      if (result) {
        db.commit(con);
        return Responses.OK;
      } else {
        db.rollback(con);
        return Responses.DataProblem.NOT_SUITABLE;
      }
    } catch (SQLException e) {
      db.rollback(con);
      log.error("Failed to import products", e);
      return Responses.DataProblem.NOT_SUITABLE;
    } finally {
      if (con != null)
        db.close(con);
    }
  }

  private ImportProduct map(ResultSet rs) {
    try {
      ImportProduct model = new ImportProduct();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setImportType(ImportType.valueOf(rs.getString("import_type")));
      model.setData(rs.getString("data"));
      model.setStatus(LinkStatus.valueOf(rs.getString("status")));
      model.setLastCheck(rs.getTimestamp("last_check"));
      model.setLastUpdate(rs.getTimestamp("last_update"));
      model.setRetry(rs.getInt("retry"));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setDescription(rs.getString("description"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set import's properties", e);
    }
    return null;
  }

}
