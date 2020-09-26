package io.inprice.api.app.competitor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CompetitorDTO;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.models.Competitor;
import io.jsonwebtoken.lang.Maps;

public class CompetitorRepository {

  private static final Logger log = LoggerFactory.getLogger(CompetitorRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public ServiceResponse findById(Long id) {
    Competitor model = 
      db.findSingle(
        String.format(
          "select l.*, s.name as platform from competitor as l " + 
          "left join site as s on s.id = l.site_id " + 
          "where l.id = %d " + 
          "  and l.company_id = %d ",
        id, CurrentUser.getCompanyId()), this::map);
    if (model != null) {
      return new ServiceResponse(model);
    }
    return Responses.NotFound.COMPETITOR;
  }

  public ServiceResponse insert(CompetitorDTO dto) {
    try (Connection con = db.getConnection()) {
      return insert(con, dto);
    } catch (Exception e) {
      log.error("Failed to insert competitor. " + dto.toString(), e);
    }
    return Responses.DataProblem.DB_PROBLEM;
  }

  public ServiceResponse insert(Connection con, CompetitorDTO dto) {
    ServiceResponse res = Responses.DataProblem.DB_PROBLEM;

    try {
      // sanitizing the url
      dto.setUrl(SqlHelper.clear(dto.getUrl()).trim());

      boolean exists = doesExistByUrl(con, dto.getUrl(), dto.getProductId());
      if (! exists) {

        String urlHash = DigestUtils.md5Hex(dto.getUrl());

        res = findSampleByHash(con, urlHash);
        if (res.isOK()) { // if any, lets clone it
          String query = 
            "insert into competitor " +
            "(url, url_hash, sku, name, brand, seller, shipment, status, http_status, website_class_name, site_id, product_id, company_id) " + 
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

          try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            Competitor sample = res.getData();

            int i = 0;
            pst.setString(++i, dto.getUrl());
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
            if (dto.getProductId() != null)
              pst.setLong(++i, dto.getProductId());
            else
              pst.setNull(++i, java.sql.Types.NULL);
            pst.setLong(++i, CurrentUser.getCompanyId());

            if (pst.executeUpdate() > 0) {
              try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                  sample.setId(generatedKeys.getLong(1));
                  res = new ServiceResponse(sample);
                }
              }
            }
          }
        } else {
          String query = "insert into competitor (url, url_hash, product_id, company_id) values  (?, ?, ?, ?) ";
          try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pst.setString(++i, dto.getUrl());
            pst.setString(++i, urlHash);
            if (dto.getProductId() != null)
              pst.setLong(++i, dto.getProductId());
            else
              pst.setNull(++i, java.sql.Types.NULL);
            pst.setLong(++i, CurrentUser.getCompanyId());

            if (pst.executeUpdate() > 0) {
              try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                  Competitor sample = new Competitor();
                  sample.setId(generatedKeys.getLong(1));
                  sample.setUrl(dto.getUrl());
                  sample.setProductId(dto.getProductId());
                  res = new ServiceResponse(sample);
                }
              }
            }
          }
        }

      } else {
        res = Responses.DataProblem.ALREADY_EXISTS;
      }
    } catch (Exception e) {
      log.error("Failed to insert competitor. " + dto.toString(), e);
    }

    return res;
  }

  private ServiceResponse findSampleByHash(Connection con, String urlHash) {
    Competitor model = 
      db.findSingle(con,
        String.format(
          "select * from competitor " + 
          "where url_hash = '%s' " + 
          "  and name is not null " + 
          "  and company_id != %d " + 
          "order by last_update desc " +
          "limit 1 ", 
          urlHash, CurrentUser.getCompanyId()), this::map);
    if (model != null) {
      return new ServiceResponse(model);
    }
    return Responses.NotFound.COMPETITOR;
  }

  public ServiceResponse deleteById(Long id) {
    String where = String.format("where competitor_id=%d and company_id=%d; ", id, CurrentUser.getCompanyId());

    List<String> queries = new ArrayList<>(4);
    queries.add("delete from competitor_price " + where);
    queries.add("delete from competitor_history " + where);
    queries.add("delete from competitor_spec " + where);
    queries.add("delete from competitor " + where.replace("competitor_", ""));

    boolean result = db.executeBatchQueries(queries, String.format("Failed to delete competitor. Id: %d", id), 1);

    if (result) {
      return Responses.OK;
    }
    return Responses.NotFound.COMPETITOR;
  }

  public ServiceResponse changeStatus(Long id, Long productId, CompetitorStatus status) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      final String q1 = 
        "update competitor " + 
        "set pre_status=status, status=?, last_update=now() " + 
        "where id=? " + 
        "  and status != ? " + 
        "  and company_id=? ";

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
            "insert into competitor_history (competitor_id, status, product_id, company_id) values (?, ?, ?, ?)")) {
          int i = 0;
          pst.setLong(++i, id);
          pst.setString(++i, status.name());
          pst.setLong(++i, productId);
          pst.setLong(++i, CurrentUser.getCompanyId());

          res2 = (pst.executeUpdate() > 0);
        }
      } else {
        log.warn("Competitor's status is already changed! Competitor Id: {}, New Status: {}", id, status);
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
      log.error("Failed to change competitor's status. Competitor Id: " + id, e);
      return Responses.ServerProblem.EXCEPTION;
    } finally {
      if (con != null)
        db.close(con);
    }
  }

  public ServiceResponse search(String term) {
    final String clearTerm = SqlHelper.clear(term);
    try {
      List<Competitor> rows = 
        db.findMultiple(
          "select id, name from competitor " + 
          "where  sku like '%" + clearTerm + "%' " + 
          "   or name like '%" + clearTerm + "%' " +
          "limit 50", this::map);
       
      return new ServiceResponse(Maps.of("rows", rows));
    } catch (Exception e) {
      log.error("Failed to search competitors. ", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public boolean doesExistByUrl(Connection con, String url, Long productId) {
    String urlHash = DigestUtils.md5Hex(url);
    Competitor model = db.findSingle(con,
        String.format(
        "select *, '' as platform from competitor " + 
        "where url_hash = '%s' " + 
        "  and company_id = %d " +
        "  and product_id " + (productId != null ? " = " + productId : " is null"),
        urlHash, CurrentUser.getCompanyId()), this::map);
    return (model != null);
  }

  private Competitor map(ResultSet rs) {
    try {
      Competitor model = new Competitor();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setUrl(rs.getString("url"));
      model.setUrlHash(rs.getString("url_hash"));
      model.setSku(rs.getString("sku"));
      model.setName(rs.getString("name"));
      model.setBrand(rs.getString("brand"));
      model.setSeller(rs.getString("seller"));
      model.setShipment(rs.getString("shipment"));
      model.setPrice(rs.getBigDecimal("price"));
      model.setPreStatus(CompetitorStatus.valueOf(rs.getString("pre_status")));
      model.setStatus(CompetitorStatus.valueOf(rs.getString("status")));
      model.setLastCheck(rs.getTimestamp("last_check"));
      model.setLastUpdate(rs.getTimestamp("last_update"));
      model.setRetry(rs.getInt("retry"));
      model.setHttpStatus(rs.getInt("http_status"));
      model.setWebsiteClassName(rs.getString("website_class_name"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setProductId(RepositoryHelper.nullLongHandler(rs, "product_id"));
      model.setSiteId(RepositoryHelper.nullLongHandler(rs, "site_id"));
      model.setPlatform(rs.getString("platform"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set competitor's properties", e);
    }
    return null;
  }

}
