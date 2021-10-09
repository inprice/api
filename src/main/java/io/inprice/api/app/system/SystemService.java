package io.inprice.api.app.system;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Plan;
import io.inprice.common.models.Workspace;

public class SystemService {

  private static final Logger logger = LoggerFactory.getLogger(SystemService.class);

  //needs to be volatile to prevent cache incoherence issues.
  private static volatile List<Plan> planList;

  Response getPlans() {
  	//double checked locking
  	if (planList == null) {
  		synchronized (SystemService.class) {
  			if (planList == null) {
          try (Handle handle = Database.getHandle()) {
            SystemDao planDao = handle.attach(SystemDao.class);
            planList = planDao.fetchPublicPlans(); 
          }
  			}
			}
    }

  	return new Response(planList);
  }

  Response refreshSession() {
    Response response = Responses.NotFound.WORKSPACE;

    try (Handle handle = Database.getHandle()) {
      WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
      response = Commons.refreshSession(workspaceDao, CurrentUser.getWorkspaceId());
    } catch (Exception e) {
      response = Responses.DataProblem.DB_PROBLEM;
      logger.error("Failed to refresh session!", e);
    }

    return response;
  }

  Response getStatistics() {
    Response response = Responses.NotFound.WORKSPACE;

    try (Handle handle = Database.getHandle()) {
      WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
      Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
      
      Integer userLimit = 0;
      Integer linkLimit = 0;
      Integer alarmLimit = 0;
      if (workspace != null && workspace.getPlan() != null) {
      	userLimit = workspace.getPlan().getUserLimit();
      	linkLimit = workspace.getPlan().getLinkLimit();
      	alarmLimit = workspace.getPlan().getAlarmLimit();
      	if (userLimit == null) userLimit = 0;
      	if (linkLimit == null) linkLimit = 0;
      	if (alarmLimit == null) alarmLimit = 0;
      }

      Map<String, Integer> data = Map.of(
	      "userLimit", userLimit,
	      "userCount", workspace.getUserCount(),
	      "remainingUser", userLimit-workspace.getUserCount(),
	      "linkLimit", linkLimit,
	      "linkCount", workspace.getLinkCount(),
	      "remainingLink", linkLimit-workspace.getLinkCount(),
	      "alarmLimit", alarmLimit,
	      "alarmCount", workspace.getAlarmCount(),
	      "remainingAlarm", alarmLimit-workspace.getAlarmCount()
      );
      response = new Response(data);
    } catch (Exception e) {
      response = Responses.DataProblem.DB_PROBLEM;
      logger.error("Failed to get statistics!", e);
    }

    return response;
  }

}
