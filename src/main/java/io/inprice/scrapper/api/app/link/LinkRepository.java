package io.inprice.scrapper.api.app.link;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.BulkDeleteStatements;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.app.product.Product;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class LinkRepository {

  private static final Logger log = LoggerFactory.getLogger(LinkRepository.class);

  private static final Database db = Beans.getSingleton(Database.class);
  private static final BulkDeleteStatements bulkDeleteStatements = Beans.getSingleton(BulkDeleteStatements.class);

  public ServiceResponse findById(Long id) {
    Link model = 
      db.findSingle(
        String.format(
          "select l.*, s.name as platform from link as l " + 
          "left join site as s on s.id = l.site_id " + 
          "where l.id = %d " + 
          "  and l.company_id = %d ",
        id, CurrentUser.getCompanyId()), this::map);
    if (model != null) {
      return new ServiceResponse(model);
    }
    return Responses.NotFound.LINK;
  }

  public ServiceResponse getList(Product product) {
    List<Link> links = db.findMultiple(
        String.format(
          "select l.*, s.name as platform from link as l " + 
          "left join site as s on s.id = l.site_id " + 
          "where product_id = %d " +
          "  and company_id = %d " +
          "order by name",
          product.getId(), CurrentUser.getCompanyId()),
        this::map);

    Map<String, Object> data = new HashMap<>(2);
    data.put("product", product);
    if (links != null && links.size() > 0) data.put("links", links);

    return new ServiceResponse(data);
  }

  public ServiceResponse insert(LinkDTO dto) {
    ServiceResponse res = Responses.DataProblem.DB_PROBLEM;

    try (Connection con = db.getConnection()) {
      boolean exists = doesExist(con, dto.getUrl(), dto.getProductId());
      if (! exists) {

        int affected = 0;
        String urlHash = DigestUtils.md5Hex(dto.getUrl());

        res = findSampleByHash(con, urlHash);
        if (res.isOK()) { // if any, lets clone it
          String query = 
            "insert into link " +
            "(url, url_hash, sku, name, brand, seller, shipment, status, http_status, website_class_name, site_id, product_id, company_id) " + 
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

          try (PreparedStatement pst = con.prepareStatement(query)) {
            Link sample = res.getData();
            
            int i = 0;
            pst.setString(++i, SqlHelper.clear(dto.getUrl()));
            pst.setString(++i, urlHash);
            pst.setString(++i, sample.getSku());
            pst.setString(++i, sample.getName());
            pst.setString(++i, sample.getBrand());
            pst.setString(++i, sample.getSeller());
            pst.setString(++i, sample.getShipment());
            pst.setString(++i, sample.getStatus().name());
            pst.setInt(++i, sample.getHttpStatus());
            pst.setString(++i, sample.getWebsiteClassName());
            pst.setLong(++i, sample.getSiteId());
            pst.setLong(++i, dto.getProductId());
            pst.setLong(++i, CurrentUser.getCompanyId());
            affected = pst.executeUpdate();
          }
        } else {
          String query = "insert into link (url, url_hash, product_id, company_id) values  (?, ?, ?, ?) ";
          try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setString(++i, SqlHelper.clear(dto.getUrl()));
            pst.setString(++i, urlHash);
            pst.setLong(++i, dto.getProductId());
            pst.setLong(++i, CurrentUser.getCompanyId());
            affected = pst.executeUpdate();
          }
        }

        if (affected > 0) {
          res = Responses.OK;
        }
      } else {
        res = Responses.DataProblem.ALREADY_EXISTS;
      }
    } catch (SQLIntegrityConstraintViolationException ie) {
      log.error("Failed to insert link (duplicate): " + ie.getMessage());
      return Responses.DataProblem.INTEGRITY_PROBLEM;
    } catch (Exception e) {
      log.error("Failed to insert link. " + dto.toString(), e);
    }

    return res;
  }

  private ServiceResponse findSampleByHash(Connection con, String urlHash) {
    Link model = 
      db.findSingle(con,
        String.format(
          "select * from link " + 
          "where url_hash = '%s' " + 
          "  and name is not null " + 
          "  and company_id != %d " + 
          "order by last_update desc " +
          "limit 1 ", 
          urlHash, CurrentUser.getCompanyId()), this::map);
    if (model != null) {
      return new ServiceResponse(model);
    }
    return Responses.NotFound.LINK;
  }

  public ServiceResponse deleteById(Long id) {
    boolean result = db.executeBatchQueries(bulkDeleteStatements.linksByLinkIdId(id),
        String.format("Failed to delete link. Id: %d", id), 1 // at least one execution must be successful
    );

    if (result)
      return Responses.OK;
    else
      return Responses.NotFound.LINK;
  }

  public ServiceResponse changeStatus(Long id, Long productId, LinkStatus status) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      final String q1 = "update link " + "set pre_status=status, status=?, last_update=now() " + "where id=? "
          + "  and status != ? " + "  and company_id=? ";

      boolean res1;
      boolean res2 = false;

      try (PreparedStatement pst = con.prepareStatement(q1)) {
        int i = 0;
        pst.setString(++i, status.name());
        pst.setLong(++i, id);
        pst.setString(++i, status.name());
        pst.setLong(++i, CurrentUser.getCompanyId());

        res1 = (pst.executeUpdate() > 0);
      }

      if (res1) {
        try (PreparedStatement pst = con.prepareStatement(
            "insert into link_history (link_id, status, product_id, company_id) " + "values (?, ?, ?, ?)")) {
          int i = 0;
          pst.setLong(++i, id);
          pst.setString(++i, status.name());
          pst.setLong(++i, productId);
          pst.setLong(++i, CurrentUser.getCompanyId());

          res2 = (pst.executeUpdate() > 0);
        }
      } else {
        log.warn("Link's status is already changed! Link Id: {}, New Status: {}", id, status);
      }

      if (res1) {
        if (res2) {
          db.commit(con);
          return Responses.OK;
        } else {
          db.rollback(con);
          return Responses.DataProblem.DB_PROBLEM;
        }
      } else {
        return Responses.NotFound.PRODUCT;
      }

    } catch (SQLException e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to change link's status. Link Id: " + id, e);
      return Responses.ServerProblem.EXCEPTION;
    } finally {
      if (con != null)
        db.close(con);
    }
  }

  private boolean doesExist(Connection con, String url, Long productId) {
    String urlHash = DigestUtils.md5Hex(url);
    Link model = db.findSingle(con,
        String.format(
        "select *, '' as platform from link " + 
        "where url_hash = '%s' " + 
        "  and product_id = %d " + 
        "  and company_id = %d ",
        urlHash, productId, CurrentUser.getCompanyId()), this::map);
    return (model != null);
  }

  private Link map(ResultSet rs) {
    try {
      Link model = new Link();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setUrl(rs.getString("url"));
      model.setUrlHash(rs.getString("url_hash"));
      model.setSku(rs.getString("sku"));
      model.setName(rs.getString("name"));
      model.setBrand(rs.getString("brand"));
      model.setSeller(rs.getString("seller"));
      model.setShipment(rs.getString("shipment"));
      model.setPrice(rs.getBigDecimal("price"));
      model.setPreStatus(LinkStatus.valueOf(rs.getString("pre_status")));
      model.setStatus(LinkStatus.valueOf(rs.getString("status")));
      model.setLastCheck(rs.getTimestamp("last_check"));
      model.setLastUpdate(rs.getTimestamp("last_update"));
      model.setRetry(rs.getInt("retry"));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setWebsiteClassName(rs.getString("website_class_name"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setProductId(RepositoryHelper.nullLongHandler(rs, "product_id"));
      model.setSiteId(RepositoryHelper.nullLongHandler(rs, "site_id"));
      model.setPlatform(rs.getString("platform"));

      // if not null then it is an imported product!
      model.setImportId(RepositoryHelper.nullLongHandler(rs, "import_id"));
      model.setImportRowId(RepositoryHelper.nullLongHandler(rs, "import_row_id"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set link's properties", e);
    }
    return null;
  }

}
