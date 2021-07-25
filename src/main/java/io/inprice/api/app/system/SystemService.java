package io.inprice.api.app.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Account;
import io.inprice.common.models.Plan;

public class SystemService {

  private static final Logger log = LoggerFactory.getLogger(SystemService.class);

  //needs to be volatile to prevent cache incoherence issues.
  private static volatile List<Plan> planList;

  Response getPlans() {
  	//double checked locking
  	if (planList == null) {
  		synchronized (SystemService.class) {
  			if (planList == null) {
          try (Handle handle = Database.getHandle()) {
            PlanDao planDao = handle.attach(PlanDao.class);
            planList = planDao.fetchPublicPlans(); 
          }
  			}
			}
    }

  	return new Response(planList);
  }

  Response refreshSession() {
    Response response = Responses.NotFound.ACCOUNT;

    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      response = Commons.refreshSession(accountDao, CurrentUser.getAccountId());
    } catch (Exception e) {
      response = Responses.DataProblem.DB_PROBLEM;
      log.error("Failed to refresh session!", e);
    }

    return response;
  }

  Response getStatistics() {
    Response response = Responses.NotFound.ACCOUNT;

    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      Account account = accountDao.findById(CurrentUser.getAccountId());
      
      Integer userLimit = 0;
      Integer linkLimit = 0;
      Integer alarmLimit = 0;
      if (account != null && account.getPlan() != null) {
      	userLimit = account.getPlan().getUserLimit();
      	linkLimit = account.getPlan().getLinkLimit();
      	alarmLimit = account.getPlan().getAlarmLimit();
      	if (userLimit == null) userLimit = 0;
      	if (linkLimit == null) linkLimit = 0;
      	if (alarmLimit == null) alarmLimit = 0;
      }

      Map<String, Integer> data = new HashMap<>(9);
      data.put("userLimit", userLimit);
      data.put("userCount", account.getUserCount());
      data.put("remainingUser", userLimit-account.getUserCount());
      
      data.put("linkLimit", linkLimit);
      data.put("linkCount", account.getLinkCount());
      data.put("remainingLink", linkLimit-account.getLinkCount());

      data.put("alarmLimit", alarmLimit);
      data.put("alarmCount", account.getAlarmCount());
      data.put("remainingAlarm", alarmLimit-account.getAlarmCount());

      response = new Response(data);
    } catch (Exception e) {
      response = Responses.DataProblem.DB_PROBLEM;
      log.error("Failed to get statistics!", e);
    }

    return response;
  }

}
