package io.inprice.api.app.dashboard;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.dashboard.mapper.Most10Product;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.meta.Position;
import io.inprice.common.utils.DateUtils;

class DashboardService {

  private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

  private final String OTHERS = "OTHERS";

  Response getReport(boolean refresh) {
    Map<String, Object> report = null;
    if (! refresh) report = RedisClient.dashboardsMap.get(CurrentUser.getAccountId());

    if (report == null) {
      report = new HashMap<>(4);

      try (Handle handle = Database.getHandle()) {
        AccountDao accountDao = handle.attach(AccountDao.class);
        DashboardDao dashboardDao = handle.attach(DashboardDao.class);

        report.put("date", DateUtils.formatLongDate(new Date()));
        report.put("products", getProducts(dashboardDao));
        report.put("links", getLinks(dashboardDao));
        report.put("account", accountDao.findById(CurrentUser.getAccountId()));

        RedisClient.dashboardsMap.put(CurrentUser.getAccountId(), report, 5, TimeUnit.MINUTES);
        return new Response(report);
    
      } catch (Exception e) {
        log.error("Failed to get dashboard report", e);
      }
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private Map<String, Object> getProducts(DashboardDao dashboardDao) {
    Map<String, Object> result = new HashMap<>(2);
    result.put("extremePrices", find10ProductsHavingExtremePrices(dashboardDao));
    result.put("positionDists", findProductPositionDists(dashboardDao));
    return result;
  }

  private Map<String, Object> getLinks(DashboardDao dashboardDao) {
    Map<String, Object> result = new HashMap<>(2);
    result.put("statusDists", findLinkStatusDists(dashboardDao));
    result.put("mru25", dashboardDao.findMR25Link(CurrentUser.getAccountId()));
    return result;
  }

  /**
   * finding link distributions by the LinkStatus
   */
  private int[] findLinkStatusDists(DashboardDao dashboardDao) {
    Map<String, Integer> stats = new HashMap<>(6);
    int i = 0;
    stats.put(LinkStatus.AVAILABLE.name(), i++);
    stats.put(LinkStatus.NOT_AVAILABLE.name(), i++);
    stats.put(LinkStatus.TOBE_IMPLEMENTED.name(), i++);
    stats.put(LinkStatus.NETWORK_ERROR.name(), i++);
    stats.put(OTHERS, i++);

    int[] result = new int[i];

    Map<String, Integer> statusDistMap = dashboardDao.findStatusDists(CurrentUser.getAccountId());
    if (statusDistMap != null && statusDistMap.size() > 0) {
      for (Entry<String, Integer> entry: statusDistMap.entrySet()) {
        Integer index = stats.get(entry.getKey());
        if (index == null) index = i-1; // it must be OTHERS's index
        result[index] += entry.getValue();
      }
    } else {
      result = null;
    }

    return result;
  }

  /**
   * finding product distributions by the Positions
   */
  private int[] findProductPositionDists(DashboardDao dashboardDao) {
    int[] result = new int[5];

    Map<Integer, Integer> positionDistMap = dashboardDao.findPositionDists(CurrentUser.getAccountId());
    if (positionDistMap != null && positionDistMap.size() > 0) {
      for (Entry<Integer, Integer> entry: positionDistMap.entrySet()) {
        result[entry.getKey()-1] = entry.getValue();
      }
    } else {
      result = null;
    }

    return result;
  }

  /**
   * finding 10 Products having lowest / highest prices
   */
  private Map<String, List<Most10Product>> find10ProductsHavingExtremePrices(DashboardDao dashboardDao) {
    Map<String, List<Most10Product>> result = new HashMap<>(2);

    Position[] positions = { Position.LOWEST, Position.HIGHEST };
    for (Position pos: positions) {
      result.put(pos.name().toLowerCase(), dashboardDao.findMost10Product(pos.ordinal()+1, CurrentUser.getAccountId()));
    }

    return result;
  }

}
