package io.inprice.api.app.system;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Database;

public class SystemService {

  Response getPlans() {
    return new Response(Plans.getPlans());
  }

  Response refreshSession() {
    Response res = Responses.NotFound.ACCOUNT;

    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      res = Commons.refreshSession(accountDao, CurrentUser.getAccountId());
    } catch (Exception e) {
      res = Responses.DataProblem.DB_PROBLEM;
    }

    return res;
  }

}
