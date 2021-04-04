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
import io.inprice.api.app.dashboard.mapper.Most10Group;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.Level;
import io.inprice.common.meta.LinkStatusGroup;
import io.inprice.common.utils.DateUtils;

class DashboardService {

  private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

  Response getReport(boolean refresh) {
    Map<String, Object> report = null;
    if (! refresh) report = RedisClient.dashboardsMap.get(CurrentUser.getAccountId());

    if (report == null) {
      report = new HashMap<>(4);

      try (Handle handle = Database.getHandle()) {
        AccountDao accountDao = handle.attach(AccountDao.class);
        DashboardDao dashboardDao = handle.attach(DashboardDao.class);

        report.put("date", DateUtils.formatLongDate(new Date()));
        report.put("groups", getGroups(dashboardDao));
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

  private Map<String, Object> getGroups(DashboardDao dashboardDao) {
    Map<String, Object> result = new HashMap<>(2);
    result.put("extremePrices", find10GroupsHavingExtremePrices(dashboardDao));
    result.put("levelDists", findGroupLevelDists(dashboardDao));
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
    stats.put(LinkStatusGroup.ACTIVE.name(), i++);
    stats.put(LinkStatusGroup.WAITING.name(), i++);
    stats.put(LinkStatusGroup.TRYING.name(), i++);
    stats.put(LinkStatusGroup.PROBLEM.name(), i++);

    int[] result = new int[i];

    Map<String, Integer> statusGroupDistMap = dashboardDao.findStatusGroupDists(CurrentUser.getAccountId());
    if (statusGroupDistMap != null && statusGroupDistMap.size() > 0) {
      for (Entry<String, Integer> entry: statusGroupDistMap.entrySet()) {
        Integer index = stats.get(entry.getKey());
        result[index] += entry.getValue();
      }
    } else {
      result = null;
    }

    return result;
  }

  /**
   * finding group distributions by the Levels
   */
  private Map<String, Integer> findGroupLevelDists(DashboardDao dashboardDao) {
    return dashboardDao.findLevelDists(CurrentUser.getAccountId());
  }

  /**
   * finding 10 Groups having lowest / highest prices
   */
  private Map<String, List<Most10Group>> find10GroupsHavingExtremePrices(DashboardDao dashboardDao) {
    Map<String, List<Most10Group>> result = new HashMap<>(2);

    Level[] selected = { Level.LOWEST, Level.HIGHEST };
    for (Level level: selected) {
      result.put(level.name(), dashboardDao.findMost10Group(level, CurrentUser.getAccountId()));
    }

    return result;
  }

}
