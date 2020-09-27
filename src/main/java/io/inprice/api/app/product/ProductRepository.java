package io.inprice.api.app.product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.lookup.LookupRepository;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductSearchDTO;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.info.ProductDTO;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.meta.LookupType;
import io.inprice.common.models.Competitor;
import io.inprice.common.models.Product;
import io.inprice.common.models.ProductPrice;
import io.jsonwebtoken.lang.Maps;

public class ProductRepository {

  //private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM");

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final LookupRepository lookupRepository = Beans.getSingleton(LookupRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private static final String BASE_QUERY = 
    "select p.*, pp.*, brand.name as brand, category.name as category from product as p " +
      "left join product_price as pp on p.last_price_id = pp.id " +
      "left join lookup as brand on p.brand_id = brand.id " +
      "left join lookup as category on p.category_id = category.id ";

  private final ProductPrice zeroPrice;

  public ProductRepository() {
    // used for products having no any available competitor
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

  public Response findById(Long id) {
    Product model = db.findSingle(
        String.format("%s where p.id=%d and p.company_id=%d ", BASE_QUERY, id, CurrentUser.getCompanyId()), this::map);
    if (model != null) {
      return new Response(model);
    }
    return Responses.NotFound.PRODUCT;
  }

  public Response findEverythingById(Long id) {
    Map<String, Object> dataMap = new HashMap<>(3);

    try (Connection con = db.getConnection()) {
      //-------------------------------------------
      // product itself
      //-------------------------------------------
      Product produdct =
        db.findSingle(con,
          String.format("%s where p.id=%d and p.company_id=%d ", BASE_QUERY, id, CurrentUser.getCompanyId()), this::map);

      dataMap.put("product", produdct);

      //-------------------------------------------
      // competitors
      //-------------------------------------------
      List<Competitor> competitors =
        db.findMultiple(con,
          String.format(
            "select l.*, s.name as platform from competitor as l " + 
            "left join site as s on s.id = l.site_id " + 
            "where product_id = %d " +
            "  and company_id = %d " +
            "order by status, seller",
            id, CurrentUser.getCompanyId()),
          this::mapCompetitor);

      dataMap.put("competitors", competitors);

      /*-------------------------------------------
        price movements of last 10
        in this section, we create data sets of prices for chart display
      -------------------------------------------
      Set<String> priceLabels = new TreeSet<>();
      Map<String, List<BigDecimal>> datasetMap = new HashMap<>(3);

      String[] prefixes = {"min", "avg", "max"};
      for (String prefix : prefixes) {
        List<Map<Date, BigDecimal>> prices =
          db.findMultiple(con,
            String.format(
              "select DATE(created_at) AS date, MAX("+prefix+"_price) from product_price " + 
              "where product_id = %d " +
              "  and company_id = %d " +
              "group by date " +
              "limit 10",
              id, CurrentUser.getCompanyId()),
            this::mapPriceMovement);

        boolean firstTime = true;
        List<BigDecimal> dataset = new ArrayList<>(11);
        if (prices != null && prices.size() > 0) {
          //dataset.add(BigDecimal.ZERO);

          //creating dataset
          for (Map<Date, BigDecimal> price : prices) {
            for (Entry<Date, BigDecimal> entry: price.entrySet()) {
              dataset.add(entry.getValue());
              if (firstTime) priceLabels.add(sdf.format(entry.getKey()));
            }
            firstTime = false;
          }
        }
        datasetMap.put(prefix, dataset);
      }
      
      dataMap.put("priceLabels", priceLabels);
      dataMap.put("priceData", datasetMap);
      */
      return new Response(dataMap);
      
    } catch (Exception e) {
      log.error("Failed to find a product by id to get everything about it", e);
    }

    return Responses.NotFound.PRODUCT;
  }

  public Response findByCode(String code) {
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

  public Response findByCode(Connection con, String code) {
    Product model = db.findSingle(String.format("%s where code='%s' and p.company_id=%d ", BASE_QUERY,
        SqlHelper.clear(code), CurrentUser.getCompanyId()), this::map);

    if (model != null) {
      return new Response(model);
    }
    return Responses.NotFound.PRODUCT;
  }

  public Response simpleSearch(String term) {
    final String clearTerm = SqlHelper.clear(term);
    try {
      List<Map<String, Object>> rows = 
        db.findMultiple(
          "select id, code, name from product " + 
          "where code like '%" + clearTerm + "%' "+ 
          "   or name like '%" + clearTerm + "%' " +
          "order by name " +
          "limit " + Consts.ROW_LIMIT_FOR_LISTS, this::nameOnlyMap);
       
      return new Response(Maps.of("rows", rows));
    } catch (Exception e) {
      log.error("Failed in simple search for products. ", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public Response fullSearch(ProductSearchDTO dto) {
    dto.setTerm(SqlHelper.clear(dto.getTerm()));

    StringBuilder criteria = new StringBuilder();
    //company
    criteria.append(" and p.company_id = ");
    criteria.append(CurrentUser.getCompanyId());

    //brand
    if (dto.getBrand() != null && dto.getBrand() > 0) {
      criteria.append(" and p.brand_id = ");
      criteria.append(dto.getBrand());
    }

    //category
    if (dto.getCategory() != null && dto.getCategory() > 0) {
      criteria.append(" and p.category_id = ");
      criteria.append(dto.getCategory());
    }

    //position is a special case so we need take care of it differently
    String posField = " pp.position ";
    String posClause = " left join product_price as pp on pp.id = p.last_price_id ";
    if (dto.getPosition() != null) {
      if (dto.getPosition() > 1) {
        posClause = " inner join product_price as pp on pp.id = p.last_price_id and pp.position = " + (dto.getPosition()-1);
      } else if (dto.getPosition() == 1) {
        posField = " 'NOT YET' as position ";
        posClause = "";
        criteria.append(" and p.last_price_id is null");
      }
    }

    //limiting
    String limit = " limit " + Consts.ROW_LIMIT_FOR_LISTS;
    if (dto.getLoadMore() && dto.getRowCount() >= Consts.ROW_LIMIT_FOR_LISTS) {
      limit = " limit " + dto.getRowCount() + ", " + Consts.ROW_LIMIT_FOR_LISTS;
    }

    try {
      List<Map<String, Object>> rows = 
        db.findMultiple(
          "select p.id, p.code, p.name, p.price, "+posField+", b.name as brand, c.name as category, p.updated_at, p.created_at from product as p " + 
          " left join lookup as b on b.id = p.brand_id and b.company_id = p.company_id and b.type = 'BRAND' " + 
          " left join lookup as c on c.id = p.category_id and c.company_id = p.company_id and c.type = 'CATEGORY' " + 
          posClause +
          " where p.name like '%" + dto.getTerm() + "%' "+ 
          criteria +
          " order by p.name " +
          limit, 
          this::mapSearch);
       
      return new Response(Maps.of("rows", rows));
    } catch (Exception e) {
      log.error("Failed in full search for products. ", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public Response insert(ProductDTO dto) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();
      dto.setCompanyId(CurrentUser.getCompanyId());

      Response result = insertANewProduct(con, dto);
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

  public Response createFromLink(Competitor link) {
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

      Response result = insertANewProduct(con, dto);
      if (result.isOK()) {
        isCompelted = db.executeQuery(String.format("delete from competitor where id=%d", link.getId()),
            String.format("Failed to delete link to be product. Id: %d", link.getId()));
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

  public Response update(ProductDTO dto) {
    Connection con = null;
    boolean result = false;

    try {
      con = db.getTransactionalConnection();

      Response res = findByCode(con, dto.getCode());
      if (!res.isOK()) {
        return Responses.NotFound.PRODUCT;
      }

      Product old = res.getData();
      if (!old.getId().equals(dto.getId())) {
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

        result = (pst.executeUpdate() > 0);

        if (result) {
          db.commit(con);
          return new Response(old.getPrice().equals(dto.getPrice()));
        } else {
          db.rollback(con);
          return Responses.DataProblem.DB_PROBLEM;
        }
      }
    } catch (Exception e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to update a product. " + dto, e);
      return Responses.ServerProblem.EXCEPTION;
    } finally {
      if (con != null) {
        db.close(con);
      }
    }
  }

  public Response deleteById(Long id) {
    String where = String.format("where product_id=%d and company_id=%d", id, CurrentUser.getCompanyId());

    List<String> queries = new ArrayList<>(7);
    queries.add("delete from competitor_price " + where);
    queries.add("delete from competitor_history " + where);
    queries.add("delete from competitor_spec " + where);
    queries.add("delete from competitor " + where);
    queries.add("delete from product_price " + where);
    queries.add("delete from product " + where.replace("product_", ""));
    queries.add(
        "update company set product_count=product_count-1 where product_count>0 and id=" + CurrentUser.getCompanyId());

    boolean result = db.executeBatchQueries(queries, String.format("Failed to delete product. Id: %d", id), 2);
    if (result) {
      return Responses.OK;
    }
    return Responses.NotFound.PRODUCT;
  }

  public Response toggleStatus(Long id) {
    boolean result = db
        .executeQuery(String.format("update product set active = not active where id=%d and company_id=%d ", id,
            CurrentUser.getCompanyId()), "Failed to toggle product status! id: " + id);

    if (result) {
      return Responses.OK;
    }
    return Responses.NotFound.PRODUCT;
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

  public Response insertANewProduct(Connection con, ProductDTO dto) {
    // increase product count by 1
    final String query1 = "update company set product_count=product_count+1 where id=? and product_count<product_limit";
    try (PreparedStatement pst1 = con.prepareStatement(query1)) {
      pst1.setLong(1, dto.getCompanyId());

      if (pst1.executeUpdate() > 0) {
        final String query2 = "insert into product (code, name, price, company_id, brand_id, category_id) values (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst2 = con.prepareStatement(query2)) {
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
            return Responses.OK;
          }
        } catch (SQLIntegrityConstraintViolationException ie) {
          return new Response("There is a product already defined with this code!");
        }
      } else {
        return Responses.PermissionProblem.PRODUCT_LIMIT_PROBLEM;
      }

    } catch (Exception e) {
      log.error("Failed to insert a  new product", e);
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private Map<String, Object> mapSearch(ResultSet rs) {
    try {
      Map<String, Object> modelMap = new HashMap<>(6);
      modelMap.put("id", RepositoryHelper.nullLongHandler(rs, "id"));
      modelMap.put("code", rs.getString("code"));
      modelMap.put("name", rs.getString("name"));
      modelMap.put("price", rs.getBigDecimal("price"));
      modelMap.put("brand", rs.getString("brand"));
      modelMap.put("category", rs.getString("category"));
      modelMap.put("position", RepositoryHelper.nullIntegerHandler(rs, "position"));
      modelMap.put("updatedAt", rs.getTimestamp("updated_at"));
      modelMap.put("createdAt", rs.getTimestamp("created_at"));
      return modelMap;
    } catch (SQLException e) {
      log.error("Failed to set name only map", e);
    }
    return null;
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
        pp.setCreatedAt(rs.getTimestamp("created_at"));
        model.setPriceDetails(pp);
      }

      return model;
    } catch (SQLException e) {
      log.error("Failed to set product's properties", e);
    }
    return null;
  }
/* 
  private Map<Date, BigDecimal> mapPriceMovement(ResultSet rs) {
    try {
      return 
        Collections.singletonMap(
          rs.getTimestamp(1),
          rs.getBigDecimal(2)
        );
    } catch (SQLException e) {
      log.error("Failed to set price movement's properties", e);
    }
    return null;
  }
 */

  private Map<String, Object> nameOnlyMap(ResultSet rs) {
    try {
      Map<String, Object> modelMap = new HashMap<>(1);
      modelMap.put("id", RepositoryHelper.nullLongHandler(rs, "id"));
      modelMap.put("code", rs.getString("code"));
      modelMap.put("name", rs.getString("name"));
      return modelMap;
    } catch (SQLException e) {
      log.error("Failed to set name only map", e);
    }
    return null;
  }

 private Competitor mapCompetitor(ResultSet rs) {
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
