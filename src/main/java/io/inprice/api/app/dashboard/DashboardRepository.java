package io.inprice.api.app.dashboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyRepository;
import io.inprice.api.helpers.HttpHelper;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.meta.Position;
import io.inprice.common.utils.DateUtils;

public class DashboardRepository {

  private static final Logger log = LoggerFactory.getLogger(DashboardRepository.class);

  private final Database db = Beans.getSingleton(Database.class);
  private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);

  public Map<String, Object> getReport() {
    Map<String, Object> report = new HashMap<>(4);

    try (Connection con = db.getConnection()) {

      report.put("date", DateUtils.formatLongDate(new Date()));
      report.put("products", getProducts(con));
      report.put("competitors", getCompetitors(con));
      report.put("company", companyRepository.findById(con, CurrentUser.getCompanyId()));

    } catch (Exception e) {
      log.error("Failed to get dashboard report", e);
    }

    return report;
  }

  private Map<String, Object> getProducts(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(2);
    result.put("extremePrices", find10ProductsHavingExtremePrices(con));
    result.put("positionDists", findProductPositionDists(con));
    return result;
  }

  private Map<String, Object> getCompetitors(Connection con) throws SQLException {
    Map<String, Object> result = new HashMap<>(2);
    result.put("statusDists", findCompetitorStatusDists(con));
    result.put("mru25", find25MRUCompetitors(con));
    return result;
  }

  /**
   * finding competitor distributions by the CompetitorStatus
   */
  private int[] findCompetitorStatusDists(Connection con) throws SQLException {
    int i = 0;
    final String OTHERS = "OTHERS";

    Map<String, Integer> stats = new HashMap<>(6);
    stats.put(CompetitorStatus.TOBE_CLASSIFIED.name(), i++);
    stats.put(CompetitorStatus.AVAILABLE.name(), i++);
    stats.put(CompetitorStatus.NOT_AVAILABLE.name(), i++);
    stats.put(CompetitorStatus.TOBE_IMPLEMENTED.name(), i++);
    stats.put(CompetitorStatus.WONT_BE_IMPLEMENTED.name(), i++);
    stats.put(OTHERS, i++);

    int[] result = new int[i];

    final String query = "select status, count(1) from competitor where company_id=? group by status";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          Integer index = stats.get(rs.getString(1));
          if (index == null) index = i-1; // it must be OTHERS's index
          result[index] += rs.getInt(2);
        }
      }
    }

    return result;
  }

  /**
   * finding 25 MRU Competitors (Most Recently Updated)
   *
   */
  private List<Map<String, Object>> find25MRUCompetitors(Connection con) throws SQLException {
    List<Map<String, Object>> result = new ArrayList<>(10);

    final String query = 
      "select l.*, p.name as product, s.name as platform, url from competitor as l " + 
      "inner join product as p on p.id = l.product_id " + 
      "left join site as s on s.id = l.site_id " + 
      "where l.company_id=? " +
      "order by l.last_update desc limit 25";

    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>(7);
          row.put("productName", rs.getString("product"));
          row.put("seller", rs.getString("seller"));
          row.put("price", rs.getBigDecimal("price"));
          row.put("status", rs.getString("status"));

          if (rs.getTimestamp("last_update") != null) {
            row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("last_update")));
          } else {
            row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("created_at")));
          }

          String platform = rs.getString("platform");
          if (StringUtils.isBlank(platform)) {
            platform = HttpHelper.extractHostname(rs.getString("url"));
          }
          row.put("platform", platform);

          result.add(row);
        }
      }
    }

    return result;
  }

  /**
   * finding 10 Products having lowest / highest prices
   *
   */
  private Map<String, List<Map<String, Object>>> find10ProductsHavingExtremePrices(Connection con) throws SQLException {
    Map<String, List<Map<String, Object>>> result = new HashMap<>(2);

    Position[] positions = { Position.LOWEST, Position.HIGHEST};
    for (Position pos: positions) {

      final String query = 
        "select p.id as pid, p.name, p.updated_at as upat, p.created_at as crat, pp.* from product as p " +
        "left join product_price as pp on pp.id = p.last_price_id " +
        "where p.company_id=? " +
        "  and pp.position= " + (pos.ordinal()+1) +
        " order by p.updated_at " + (pos.equals(Position.HIGHEST) ? "desc" : "") + " limit 10";

      List<Map<String, Object>> rows = new ArrayList<>(10);
      try (PreparedStatement pst = con.prepareStatement(query)) {
        pst.setLong(1, CurrentUser.getCompanyId());

        try (ResultSet rs = pst.executeQuery()) {
          while (rs.next()) {
            Map<String, Object> row = new HashMap<>(7);
            row.put("id", rs.getString("pid"));
            row.put("name", rs.getString("name"));
            row.put("price", rs.getBigDecimal("price"));
            row.put("competitors", rs.getInt("competitors"));
            row.put("ranking", rs.getInt("ranking"));
            row.put("rankingWith", rs.getInt("ranking_with"));
            if (rs.getTimestamp("upat") != null) {
              row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("upat")));
            } else {
              row.put("lastUpdate", DateUtils.formatLongDate(rs.getTimestamp("crat")));
            }
            rows.add(row);
          }
        }
      }

      result.put(pos.name().toLowerCase(), rows);
    }

    return result;
  }

  /**
   * finding product distributions by the Positions
   */
  private int[] findProductPositionDists(Connection con) throws SQLException {
    int[] result = new int[5];

    final String query = 
      "select pp.position, count(1) " +
      "from product as p " +
      "inner join product_price as pp on pp.id = p.last_price_id " +
      "where p.last_price_id is not null " +
      "  and p.company_id=? " +
      "group by pp.position";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      pst.setLong(1, CurrentUser.getCompanyId());

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          result[rs.getInt(1)-1] = rs.getInt(2);
        }
      }
    }

    return result;
  }

}
