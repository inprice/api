package io.inprice.api.app.dashboard;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.app.dashboard.mapper.ProductSummary;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.lib.ExpiringHashMap;
import io.inprice.common.lib.ExpiringMap;
import io.inprice.common.meta.Position;
import io.inprice.common.meta.Grup;
import io.inprice.common.utils.DateUtils;

class DashboardService {

  private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

  private ExpiringMap<Long, Map<String, Object>> expiringMap = new ExpiringHashMap<>(5 * 60 * 1000); //expires in 5 mins

  Response getReport(boolean refresh) {
    Map<String, Object> report = null;
    if (! refresh) report = expiringMap.get(CurrentUser.getWorkspaceId());

    if (report == null) {
      report = new HashMap<>(4);

      try (Handle handle = Database.getHandle()) {
        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        DashboardDao dashboardDao = handle.attach(DashboardDao.class);

        report.put("date", DateUtils.formatLongDate(new Date()));
        report.put("products", getProducts(dashboardDao));
        report.put("links", getLinks(dashboardDao));
        report.put("workspace", workspaceDao.findById(CurrentUser.getWorkspaceId()));

        expiringMap.put(CurrentUser.getWorkspaceId(), report);
        return new Response(report);
    
      } catch (Exception e) {
        logger.error("Failed to get dashboard report", e);
      }
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private Map<String, Object> getProducts(DashboardDao dashboardDao) {
    Map<String, Object> result = new HashMap<>(2);
    result.put("positionSeries", findProductPositionSeries(dashboardDao));
    result.put("extremePrices", findNProductsHavingExtremePrices(dashboardDao));
    return result;
  }

  private Map<String, Object> getLinks(DashboardDao dashboardDao) {
  	Map<String, Object> result = new HashMap<>(3);
  	result.put("grupSeries", findGrupSeries(dashboardDao));
  	result.put("positionSeries", findLinkPositionSeries(dashboardDao));
		result.put("mru25", dashboardDao.findMR25Link(CurrentUser.getWorkspaceId()));
    return result;
  }

  /**
   * finding link distributions by the LinkStatus
   */
  private int[] findGrupSeries(DashboardDao dashboardDao) {
  	int i = 0;
    Map<String, Integer> stats = Map.of(
    	Grup.ACTIVE.name(), i++,
    	Grup.WAITING.name(), i++,
    	Grup.TRYING.name(), i++,
    	Grup.PROBLEM.name(), i++
  	);

    int[] result = new int[i];

    Map<String, Integer> grupDistMap = dashboardDao.findGrupDists(CurrentUser.getWorkspaceId());
    if (MapUtils.isNotEmpty(grupDistMap)) {
      for (Entry<String, Integer> entry: grupDistMap.entrySet()) {
        Integer index = stats.get(entry.getKey());
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
  private int[] findProductPositionSeries(DashboardDao dashboardDao) {
    Map<String, Integer> positionDistMap = dashboardDao.findProductPositionDists(CurrentUser.getWorkspaceId());
    return findSeries(positionDistMap, dashboardDao);
  }

  /**
   * finding link distributions by the Positions
   */
  private int[] findLinkPositionSeries(DashboardDao dashboardDao) {
    Map<String, Integer> positionDistMap = dashboardDao.findLinkPositionDists(CurrentUser.getWorkspaceId());
    return findSeries(positionDistMap, dashboardDao);
  }
  
  /**
   * finding N (means the number) Products having lowest / highest prices
   */
  private Map<String, List<ProductSummary>> findNProductsHavingExtremePrices(DashboardDao dashboardDao) {
  	Map<String, List<ProductSummary>> result = new HashMap<>(2);
  	
  	Position[] selected = { Position.Lowest, Position.Highest };
  	for (Position position: selected) {
  		result.put(position.name(), dashboardDao.findMostNProduct(5, position, CurrentUser.getWorkspaceId()));
  	}
  	
  	return result;
  }

  private int[] findSeries(Map<String, Integer> dataMap, DashboardDao dashboardDao) {
  	int i = 0;
    Map<String, Integer> stats = Map.of(
    	Position.Lowest.name(), i++,
    	Position.Low.name(), i++,
    	Position.Average.name(), i++,
    	Position.High.name(), i++,
    	Position.Highest.name(), i++,
    	Position.Equal.name(), i++,
    	Position.NotSet.name(), i++
  	);

    int[] result = new int[i];

    if (MapUtils.isNotEmpty(dataMap)) {
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
