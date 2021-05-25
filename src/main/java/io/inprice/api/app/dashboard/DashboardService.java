package io.inprice.api.app.dashboard;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.dashboard.mapper.GroupSummary;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.lib.ExpiringConcurrentHashMap;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.Level;
import io.inprice.common.meta.LinkStatusGroup;
import io.inprice.common.utils.DateUtils;

class DashboardService {

  private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

  private Map<Long, Map<String, Object>> expiringMap = new ExpiringConcurrentHashMap<>(5 * 60 * 1000); //expires in 5 mins

  Response getReport(boolean refresh) {
  	if (CurrentUser.getAccountId() == null) return Responses._401;

    Map<String, Object> report = null;
    if (! refresh) report = expiringMap.get(CurrentUser.getAccountId());

    if (report == null) {
      report = new HashMap<>(4);

      try (Handle handle = Database.getHandle()) {
        AccountDao accountDao = handle.attach(AccountDao.class);
        DashboardDao dashboardDao = handle.attach(DashboardDao.class);

        report.put("date", DateUtils.formatLongDate(new Date()));
        report.put("groups", getGroups(dashboardDao));
        report.put("links", getLinks(dashboardDao));
        report.put("account", accountDao.findById(CurrentUser.getAccountId()));

        expiringMap.put(CurrentUser.getAccountId(), report);
        return new Response(report);
    
      } catch (Exception e) {
        log.error("Failed to get dashboard report", e);
      }
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private Map<String, Object> getGroups(DashboardDao dashboardDao) {
    Map<String, Object> result = new HashMap<>(2);
    result.put("levelSeries", findGroupLevelSeries(dashboardDao));
    result.put("extremePrices", findNGroupsHavingExtremePrices(dashboardDao));
    return result;
  }

  private Map<String, Object> getLinks(DashboardDao dashboardDao) {
    Map<String, Object> result = new HashMap<>(3);
    result.put("statusGroupSeries", findLinkStatusGroupSeries(dashboardDao));
    result.put("levelSeries", findLinkLevelSeries(dashboardDao));
    result.put("mru25", dashboardDao.findMR25Link(CurrentUser.getAccountId()));
    return result;
  }

  /**
   * finding link distributions by the LinkStatus
   */
  private int[] findLinkStatusGroupSeries(DashboardDao dashboardDao) {
    Map<String, Integer> stats = new HashMap<>(4);
    int i = 0;
    stats.put(LinkStatusGroup.ACTIVE.name(), i++);
    stats.put(LinkStatusGroup.TRYING.name(), i++);
    stats.put(LinkStatusGroup.WAITING.name(), i++);
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
  private int[] findGroupLevelSeries(DashboardDao dashboardDao) {
    Map<String, Integer> levelDistMap = dashboardDao.findGroupLevelDists(CurrentUser.getAccountId());
    return findSeries(levelDistMap, dashboardDao);
  }

  /**
   * finding link distributions by the Levels
   */
  private int[] findLinkLevelSeries(DashboardDao dashboardDao) {
    Map<String, Integer> levelDistMap = dashboardDao.findLinkLevelDists(CurrentUser.getAccountId());
    return findSeries(levelDistMap, dashboardDao);
  }
  
  /**
   * finding N (means the number) Groups having lowest / highest prices
   */
  private Map<String, List<GroupSummary>> findNGroupsHavingExtremePrices(DashboardDao dashboardDao) {
  	Map<String, List<GroupSummary>> result = new HashMap<>(2);
  	
  	Level[] selected = { Level.LOWEST, Level.HIGHEST };
  	for (Level level: selected) {
  		result.put(level.name(), dashboardDao.findMostNGroup(5, level, CurrentUser.getAccountId()));
  	}
  	
  	return result;
  }

  private int[] findSeries(Map<String, Integer> dataMap, DashboardDao dashboardDao) {
    Map<String, Integer> stats = new HashMap<>(7);
    int i = 0;
    stats.put(Level.LOWEST.name(), i++);
    stats.put(Level.HIGHEST.name(), i++);
    stats.put(Level.LOWER.name(), i++);
    stats.put(Level.AVERAGE.name(), i++);
    stats.put(Level.HIGHER.name(), i++);
    stats.put(Level.EQUAL.name(), i++);
    stats.put(Level.NA.name(), i++);

    int[] result = new int[i];

    if (dataMap != null && dataMap.size() > 0) {
      for (Entry<String, Integer> entry: dataMap.entrySet()) {
        Integer index = stats.get(entry.getKey());
        result[index] += entry.getValue();
      }
    } else {
      result = null;
    }

    return result;
  }

}
