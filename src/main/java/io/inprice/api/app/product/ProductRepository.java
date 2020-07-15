package io.inprice.api.app.product;

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

import io.inprice.api.app.lookup.LookupRepository;
import io.inprice.api.consts.Responses;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.SearchModel;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.ProductDTO;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.meta.LookupType;
import io.inprice.common.models.Competitor;
import io.inprice.common.models.Product;
import io.inprice.common.models.ProductPrice;

public class ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final LookupRepository lookupRepository = Beans.getSingleton(LookupRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private static final 
    String BASE_QUERY = 
      "select p.*, pp.*, brand.name as brand, category.name as category from product as p " +
      "left join product_price as pp on p.last_price_id = pp.id " +
      "left join lookup as brand on p.brand_id = brand.id " +
      "left join lookup as category on p.category_id = category.id " ;

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
    Connection con = null;
    try {
      con = db.getConnection();
      return findByCode(con, code);
    } catch (SQLException e) {
      log.error("Failed to get a product by code", e);
    } finally {
      if (con != null) {
        try {
          con.close();
        } catch (SQLException e) {
          log.error("Failed to get a product by code", e);
        }
      }
    }
    return Responses.DataProblem.DB_PROBLEM;
  }

  public ServiceResponse findByCode(Connection con, String code) {
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
      dto.setCompanyId(CurrentUser.getCompanyId());

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

  public ServiceResponse createFromLink(Competitor link) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      ProductDTO dto = new ProductDTO();
      dto.setCode(link.getSku());
      dto.setName(link.getName());
      dto.setBrandId(lookupRepository.add(con, LookupType.BRAND, link.getBrand()).getId());
      dto.setPrice(link.getPrice());
      dto.setCompanyId(link.getCompanyId());

      boolean isCompelted = false;

      ServiceResponse result = insertANewProduct(con, dto);
      if (result.isOK()) {
        isCompelted = db.executeQuery(String.format("delete from competitor where id=%d", link.getId()),
          String.format("Failed to delete link to be product. Id: %d", link.getId())
        );
      }

      if (isCompelted) {
        db.commit(con);
        return Responses.OK;
      } else {
        db.rollback(con);
        return result;
      }

    } catch (Exception e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to insert a new product. " + link.toString(), e);
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

      ServiceResponse res = findByCode(con, dto.getCode());
      if (! res.isOK()) {
        return Responses.NotFound.PRODUCT;
      }

      Product old = res.getData();
      if (! old.getId().equals(dto.getId())) {
        return Responses.DataProblem.ALREADY_EXISTS;
      }

      final String query = "update product set code=?, name=?, brand_id=?, category_id=?, price=? where id=? and company_id=? ";

      try (PreparedStatement pst = con.prepareStatement(query)) {
        int i = 0;
        pst.setString(++i, dto.getCode());
        pst.setString(++i, dto.getName());

        if (dto.getBrandId() != null) {
          pst.setLong(++i, dto.getBrandId());
        } else {
          pst.setNull(++i, java.sql.Types.NULL);
        }
        if (dto.getCategoryId() != null) {
          pst.setLong(++i, dto.getCategoryId());
        } else {
          pst.setNull(++i, java.sql.Types.NULL);
        }

        pst.setBigDecimal(++i, dto.getPrice());
        pst.setLong(++i, dto.getId());
        pst.setLong(++i, CurrentUser.getCompanyId());

        if (pst.executeUpdate() > 0) {
          result = addAPriceHistory(con, dto);
        }

        if (result) {
          db.commit(con);
          Boolean hasPriceChanged = (! old.getPrice().equals(dto.getPrice()));
          return new ServiceResponse(hasPriceChanged);
        } else {
          db.rollback(con);
          return Responses.DataProblem.DB_PROBLEM;
        }
      }
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

    List<String> queries = new ArrayList<>(7);
    queries.add("delete from competitor_price " + where);
    queries.add("delete from competitor_history " + where);
    queries.add("delete from competitor_spec " + where);
    queries.add("delete from competitor " + where);
    queries.add("delete from product_price " + where);
    queries.add("delete from product " + where.replace("product_", ""));
    queries.add("update company set product_count=product_count-1 where product_count>0 and id=" + CurrentUser.getCompanyId());

    boolean result = db.executeBatchQueries(queries, String.format("Failed to delete product. Id: %d", id), 2);
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
      pst.setLong(++i, dto.getCompanyId());

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
    // increase product count by 1
    final String query1 = "update company set product_count=product_count+1 where id=? and product_count<product_limit";
    try (PreparedStatement pst1 = con.prepareStatement(query1)) {
      pst1.setLong(1, dto.getCompanyId());

      if (pst1.executeUpdate() > 0) {
        final String query2 = 
          "insert into product " +
          "(code, name, price, company_id, brand_id, category_id) " + 
          "values (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst2 = con.prepareStatement(query2, Statement.RETURN_GENERATED_KEYS)) {
          int i = 0;
          pst2.setString(++i, dto.getCode());
          pst2.setString(++i, dto.getName());
          pst2.setBigDecimal(++i, dto.getPrice());
          pst2.setLong(++i, dto.getCompanyId());

          if (dto.getBrandId() != null) {
            pst2.setLong(++i, dto.getBrandId());
          } else {
            pst2.setNull(++i, java.sql.Types.NULL);
          }
          if (dto.getCategoryId() != null) {
            pst2.setLong(++i, dto.getCategoryId());
          } else {
            pst2.setNull(++i, java.sql.Types.NULL);
          }
    
          if (pst2.executeUpdate() > 0) {
            try (ResultSet generatedKeys = pst2.getGeneratedKeys()) {
              if (generatedKeys.next()) {
                dto.setId(generatedKeys.getLong(1));
                if (addAPriceHistory(con, dto)) return Responses.OK;
              }
            }
          }
        } catch (SQLIntegrityConstraintViolationException ie) {
          return Responses.DataProblem.DUPLICATE;
        }
      } else {
        return Responses.PermissionProblem.PRODUCT_LIMIT_PROBLEM;
      }
  
    } catch (Exception e) {
      log.error("Failed to insert a  new product", e);
    }

    return Responses.DataProblem.DB_PROBLEM;
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
      model.setBrandId(RepositoryHelper.nullLongHandler(rs, "brand_id"));
      model.setCategoryId(RepositoryHelper.nullLongHandler(rs, "category_id"));
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
