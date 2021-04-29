package io.inprice.api.app.system;

import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Account;

public class SystemService {

  private static final Logger log = LoggerFactory.getLogger(SystemService.class);
	
  Response getPlans() {
    return new Response(Plans.getPlans());
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

      Map<String, Integer> data = new HashMap<>(3);
      data.put("linkLimit", account.getLinkLimit());
      data.put("linkCount", account.getLinkCount());
      data.put("remainingLink", account.getLinkLimit()-account.getLinkCount());

      response = new Response(data);
    } catch (Exception e) {
      response = Responses.DataProblem.DB_PROBLEM;
      log.error("Failed to get statistics!", e);
    }

    return response;
  }

}
