package io.inprice.api.app.superuser.dashboard;

/**
 * 
 * @since 2021-12-15
 * @author mdpinar
*/
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.Pair;
import io.inprice.common.lib.ExpiringHashMap;
import io.inprice.common.lib.ExpiringMap;

class DashboardService {

  private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

  private ExpiringMap<Long, Map<String, Object>> expiringMap = new ExpiringHashMap<>(5 * 60 * 1000); //expires in 5 mins

  Response getReport(boolean refresh) {
    Map<String, Object> report = null;
    if (! refresh) report = expiringMap.get(0L);

    if (report == null) {
      report = new HashMap<>(4);

      try (Handle handle = Database.getHandle()) {
        DashboardDao dao = handle.attach(DashboardDao.class);

        report.put("linkStatusCounts", addTotal(dao.findLinkStatusCounts()));
        report.put("linkGrupCounts", addTotal(dao.findLinkGrupCounts()));

        report.put("ticketStatusCounts", addTotal(dao.findTicketStatusCounts()));
        report.put("ticketPriorityCounts", addTotal(dao.findTicketPriorityCounts()));
        report.put("ticketTypeCounts", addTotal(dao.findTicketTypeCounts()));
        report.put("ticketSubjectCounts", addTotal(dao.findTicketSubjectCounts()));

        report.put("userTimezoneCounts", addTotal(dao.findUserTimezoneCounts()));
        report.put("productPositionCounts", addTotal(dao.findProductPositionCounts()));
        report.put("workspaceStatusCounts", addTotal(dao.findWorkspaceStatusCounts()));

        //report.put("platformCounts", addTotal(dao.findPlatformCounts()));

        expiringMap.put(0L, report);
        return new Response(report);
    
      } catch (Exception e) {
        logger.error("Failed to get dashboard report", e);
      }
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private List<Pair<String, Integer>> addTotal(List<Pair<String, Integer>> input) {
    if (CollectionUtils.isNotEmpty(input)) {
      int total = 0;
      for (Pair<String, Integer> pair: input) {
        total += pair.getRight();
      }
      if (total > 0) {
        input.add(new Pair<String,Integer>("Total", total));
      }
    }
    return input;
  }

}
