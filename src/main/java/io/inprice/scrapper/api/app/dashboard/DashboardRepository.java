package io.inprice.scrapper.api.app.dashboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.utils.DateUtils;

public class DashboardRepository {

  private static final Logger log = LoggerFactory.getLogger(DashboardRepository.class);

  private final Database db = Beans.getSingleton(Database.class);

  public Map<String, Object> getReport() {
    Map<String, Object> report = new HashMap<>(4);

    try (Connection con = db.getConnection()) {

      report.put("date", DateUtils.formatLongDate(new Date()));
      report.put("company", getCompanyInfo(con));
      report.put("products", getProductsInfo(con));
      report.put("links", getLinksInfo(con));

    } catch (Exception e) {
      log.error("Failed to get dashboard report", e);
    }

    return report;
  }

  /**
   * getting company info
   * 
   */
  private Map<String, Object> getCompanyInfo(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(5);
    result.put("name", CurrentUser.getCompanyName());
    result.put("lastCollectingStatus", "Waiting");
    result.put("planName", "Please select one!");
    result.put("rowLimit", 0);
    result.put("dueDate", "");

    final String 
      query = 
        "select c.*, p.row_limit " +
        "from company as c left join plan as p on c.plan_name = p.name " +
        "where c.id=?";

    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          Date lastCollectionTime = rs.getTimestamp("last_collecting_time");
          if (lastCollectionTime != null) {
            result.put("planName", rs.getString("plan_name"));
            result.put("lastCollectingTime", DateUtils.formatLongDate(lastCollectionTime));
            result.put("lastCollectingStatus", (rs.getBoolean("last_collecting_status") ? "Successful" : "Failed"));
          }
          result.put("dueDate", DateUtils.formatLongDate(rs.getTimestamp("due_date")));
        }
      }
    }

    return result;
  }

  private Map<String, Object> getProductsInfo(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(4);
    result.put("count", findProductCounts(con));
    result.put("distribution", findProductDistributions(con));
    result.put("mru10", find10MRUProducts(con));
    result.put("edgeOfYou", findMinMaxProductCountsOfYou(con));
    return result;
  }

  private Map<String, Object> getLinksInfo(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(2);
    result.put("count", findLinkCounts(con));
    result.put("distribution", findLinkDistributions(con));
    result.put("mru10", find10MRULinks(con));
    return result;
  }

  private int findLinkCounts(Connection con) throws SQLException {
    int result = 0;

    final String query = "select count(1) from link where company_id=?";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          result = rs.getInt(1);
        }
      }

    }

    return result;
  }

  private Map<String, Object> findLinkDistributions(Connection con) throws SQLException {
    final String OTHER = "OTHER";

    LinkStatus[] statuses = {
      LinkStatus.NEW,
      LinkStatus.AVAILABLE,
      LinkStatus.NOT_AVAILABLE,
      LinkStatus.PAUSED,
      LinkStatus.IMPLEMENTED,
      LinkStatus.BE_IMPLEMENTED,
      LinkStatus.WONT_BE_IMPLEMENTED
    };

    Map<String, Integer> vals = new HashMap<>(statuses.length+1);
    for (LinkStatus ls: statuses) {
      vals.put(ls.name(), 0);
    }
    vals.put(OTHER, 0);

    final String query = "select status, count(1) from link where company_id=? group by status";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString(1);
          Integer val = vals.get(name);
          if (val == null) {
            name = OTHER;
            val = vals.get(name);
          }
          if (val == null) val = 0;
          val += rs.getInt(2);
          vals.put(name, val);
        }
      }
    }

    List<String> labels = new ArrayList<>(statuses.length+1);
    List<Integer> series = new ArrayList<>(statuses.length+1);

    for (LinkStatus ls: statuses) {
      labels.add(WordUtils.capitalize(ls.name().replaceAll("_", " ")));
      series.add(vals.get(ls.name()));
    }
    labels.add(OTHER);
    series.add(vals.get(OTHER));

    Map<String, Object> result = new HashMap<>();
    result.put("labels", labels);
    result.put("series", series);

    return result;
  }

  /**
   * finding 10 MRU Links (Most Recently Updated)
   *
   */
  private List<Map<String, Object>> find10MRULinks(Connection con) throws SQLException {
    List<Map<String, Object>> result = new ArrayList<>(10);

    final String query = 
      "select l.*, p.name as product, s.name as platform from link as l " + 
      "inner join product as p on p.id = l.product_id " + 
      "left join site as s on s.id = l.site_id " + 
      "where l.company_id=? " +
      "order by l.last_update desc limit 10";

    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>(8);
          row.put("productName", rs.getString("product"));
          row.put("name", rs.getString("name"));
          row.put("platform", rs.getString("platform"));
          row.put("seller", rs.getString("seller"));
          row.put("price", rs.getBigDecimal("price"));
          row.put("status", rs.getString("status"));
          if (rs.getTimestamp("last_update") != null) {
            row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("last_update")));
          } else {
            row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("created_at")));
          }
          if (StringUtils.isNotBlank(rs.getString("sku"))) {
            row.put("sku", rs.getString("sku"));
          } else {
            row.put("sku", "NA-" + rs.getLong("id"));
          }
          result.add(row);
        }
      }
    }

    return result;
  }

  /**
   * finding product counts
   *
   */
  private Map<String, Object> findProductCounts(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(2);
    result.put("active", 0);
    result.put("passive", 0);

    final String query = "select active, count(1) from product where company_id=? group by active";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          result.put((rs.getBoolean(1) ? "active" : "passive"), rs.getInt(2));
        }
      }
    }

    return result;
  }

  /**
   * finding 10 MRU Products (Most Recently Updated)
   *
   */
  private List<Map<String, Object>> find10MRUProducts(Connection con) throws SQLException {
    List<Map<String, Object>> result = new ArrayList<>(10);

    final String query = "select * from product where company_id=? order by updated_at desc limit 10";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>(11);
          row.put("name", rs.getString("name"));
          row.put("position", rs.getInt("position"));
          row.put("price", rs.getBigDecimal("price"));
          row.put("avgPrice", rs.getBigDecimal("avg_price"));
          row.put("minPlatform", rs.getString("min_platform"));
          row.put("minSeller", rs.getString("min_seller"));
          row.put("minPrice", rs.getBigDecimal("min_price"));
          row.put("maxPlatform", rs.getString("max_platform"));
          row.put("maxSeller", rs.getString("max_seller"));
          row.put("maxPrice", rs.getBigDecimal("max_price"));
          if (rs.getTimestamp("updated_at") != null) {
            row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
          } else {
            row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("created_at")));
          }
          result.add(row);
        }
      }
    }

    return result;
  }

  /**
   * finding product distributions
   */
  private Map<String, Object> findProductDistributions(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(5);

    String[] labels = {"LOWEST", "LOWER", "AVERAGE", "HIGHER", "HIGHEST"};
    int[] series = new int[labels.length];

    final String query = "select position, count(1) from product where company_id=? and active=true group by position";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          series[rs.getInt(1)-1] = rs.getInt(2);
        }
      }
    }

    result.put("labels", labels);
    result.put("series", series);

    return result;
  }

  /**
   * finding product counts of you in which you are either the cheapest or the most
   * expensive
   */
  private Map<String, Object> findMinMaxProductCountsOfYou(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(2);

    String[] indicators = { "min", "max" };
    for (String indicator : indicators) {
      result.put(indicator, 0);

      final String query = "select count(1) from product where company_id=? and active=true and " + indicator
          + "_seller='You'";
      try (PreparedStatement pst = con.prepareStatement(query)) {
        pst.setLong(1, CurrentUser.getCompanyId());
        try (ResultSet rs = pst.executeQuery()) {
          if (rs.next()) {
            result.put(indicator, rs.getInt(1));
          }
        }
      }
    }

    return result;
  }

}
