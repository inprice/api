package io.inprice.scrapper.api.app.product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.info.ProductDTO;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.common.models.Product;
import io.inprice.scrapper.common.models.ProductPrice;

public class ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private static final String BASE_QUERY = "select p.*, pp.* from product as p left join product_price as pp on p.last_price_id = pp.id ";
  private final ProductPrice zeroPrice;

  public ProductRepository() {
    //used for products having no any available competitor
    zeroPrice = new ProductPrice();
    zeroPrice.setPrice(BigDecimal.ZERO);
    zeroPrice.setMinPlatform("NA");
    zeroPrice.setMinSeller("NA");
    zeroPrice.setMinPrice(BigDecimal.ZERO);
    zeroPrice.setMinDiff(BigDecimal.ZERO);
    zeroPrice.setAvgPrice(BigDecimal.ZERO);
    zeroPrice.setAvgDiff(BigDecimal.ZERO);
    zeroPrice.setMaxPlatform("NA");
    zeroPrice.setMaxSeller("NA");
    zeroPrice.setMaxPrice(BigDecimal.ZERO);
    zeroPrice.setMaxDiff(BigDecimal.ZERO);
    zeroPrice.setCompetitors(0);
    zeroPrice.setPosition(3);
    zeroPrice.setRanking(0);
    zeroPrice.setRankingWith(0);
    zeroPrice.setSuggestedPrice(BigDecimal.ZERO);
  }

  public ServiceResponse findById(Long id) {
    Product model = db.findSingle(
        String.format("%s where p.id=%d and p.company_id=%d ", BASE_QUERY, id, CurrentUser.getCompanyId()),
        this::map);
    if (model != null) {
      return new ServiceResponse(model);
    }
    return Responses.NotFound.PRODUCT;
  }

  public ServiceResponse findByCode(String code) {
    Product model = db.findSingle(String.format("%s where code='%s' and p.company_id=%d ",
      BASE_QUERY, SqlHelper.clear(code), CurrentUser.getCompanyId()), this::map);
    if (model != null) {
      return new ServiceResponse(model);
    }
    return Responses.NotFound.PRODUCT;
  }

  public ServiceResponse search(SearchModel searchModel) {
    searchModel.setPrefixForCompanyId("p");
    searchModel.setQuery(BASE_QUERY);
    final String searchQuery = SqlHelper.generateSearchQuery(searchModel);

    try {
      List<Product> rows = db.findMultiple(searchQuery, this::map);
      return new ServiceResponse(Maps.immutableEntry("rows", rows));
    } catch (Exception e) {
      log.error("Failed to search products. ", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public ServiceResponse insert(ProductDTO dto) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      boolean alreadyExists = exists(con, dto.getCode(), null);
      if (alreadyExists) {
        return Responses.DataProblem.ALREADY_EXISTS;
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

      boolean alreadyExists = exists(con, dto.getCode(), dto.getId());
      if (alreadyExists) {
        return Responses.DataProblem.ALREADY_EXISTS;
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
    String where = String.format("where product_id=%d and company_id=%d", id, CurrentUser.getCompanyId());

    List<String> queries = new ArrayList<>(6);
    queries.add("delete from competitor_price " + where);
    queries.add("delete from competitor_history " + where);
    queries.add("delete from competitor_spec " + where);
    queries.add("delete from competitor " + where);
    queries.add("delete from product_price " + where);
    queries.add("delete from product " + where.replace("product_", ""));

    boolean result = db.executeBatchQueries(queries, String.format("Failed to delete product. Id: %d", id), 1);

    if (result) {
      return Responses.OK;
    }
    return Responses.NotFound.PRODUCT;
  }

  public ServiceResponse toggleStatus(Long id) {
    boolean result = db
      .executeQuery(String.format("update product set active = not active where id=%d and company_id=%d ", id,
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
          .prepareStatement("select count(id) from import_product where status=? and company_id=?")) {
        pst.setString(1, CompetitorStatus.TOBE_CLASSIFIED.name());
        pst.setLong(2, CurrentUser.getCompanyId());

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
          result += rs.getInt(1);
        }
        rs.close();
      }

    } catch (Exception e) {
      log.error("Failed in finding product count", e);
    }
    return result;
  }

  public ServiceResponse insertANewProduct(Connection con, ProductDTO dto) {
    final String query = "insert into product "
        + "(code, name, brand, category, price, company_id) values (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      int i = 0;
      pst.setString(++i, dto.getCode());
      pst.setString(++i, dto.getName());
      pst.setString(++i, dto.getBrand());
      pst.setString(++i, dto.getCategory());
      pst.setBigDecimal(++i, dto.getPrice());
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
      return Responses.DataProblem.DUPLICATE;
    } catch (Exception e) {
      log.error("Error", e);
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private boolean exists(Connection con, String code, Long id) {
    String query = "select id from product where code=? and company_id=? " + (id != null ? " and id != " + id : "");

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

  private Product map(ResultSet rs) {
    try {
      Product model = new Product();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setActive(rs.getBoolean("active"));
      model.setCode(rs.getString("code"));
      model.setName(rs.getString("name"));
      model.setBrand(rs.getString("brand"));
      model.setCategory(rs.getString("category"));
      model.setPrice(rs.getBigDecimal("price"));
      model.setLastPriceId(RepositoryHelper.nullLongHandler(rs, "last_price_id"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setUpdatedAt(rs.getTimestamp("updated_at"));
      model.setCreatedAt(rs.getTimestamp("created_at"));
      model.setPriceDetails(zeroPrice);

      if (model.getLastPriceId() != null) {
        ProductPrice pp = new ProductPrice();
        pp.setPrice(rs.getBigDecimal("price"));
        pp.setMinPlatform(rs.getString("min_platform"));
        pp.setMinSeller(rs.getString("min_seller"));
        pp.setMinPrice(rs.getBigDecimal("min_price"));
        pp.setMinDiff(rs.getBigDecimal("min_diff"));
        pp.setAvgPrice(rs.getBigDecimal("avg_price"));
        pp.setAvgDiff(rs.getBigDecimal("avg_diff"));
        pp.setMaxPlatform(rs.getString("max_platform"));
        pp.setMaxSeller(rs.getString("max_seller"));
        pp.setMaxPrice(rs.getBigDecimal("max_price"));
        pp.setMaxDiff(rs.getBigDecimal("max_diff"));
        pp.setCompetitors(rs.getInt("competitors"));
        pp.setPosition(rs.getInt("position"));
        pp.setRanking(rs.getInt("ranking"));
        pp.setRankingWith(rs.getInt("ranking_with"));
        pp.setSuggestedPrice(rs.getBigDecimal("suggested_price"));
        model.setPriceDetails(pp);
      }

      return model;
    } catch (SQLException e) {
      log.error("Failed to set product's properties", e);
    }
    return null;
  }

}
