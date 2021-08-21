package io.inprice.api.helpers;

import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForResponse;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Account;
import io.javalin.http.Context;

public class Commons {

  public static Response createResponse(Context ctx, Response serviceResponse) {
    ctx.status(HttpStatus.OK_200);
    return serviceResponse;
  }

  public static Response refreshSession(Long accountId) {
    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      return refreshSession(accountDao, accountId);
    }
  }

  public static Response refreshSession(AccountDao accountDao, Long accountId) {
    Account account = accountDao.findById(accountId);
    return refreshSession(account);
  }

  public static Response refreshSession(Account account) {
    ForResponse session = new ForResponse(
      account,
      CurrentUser.getUserName(),
      CurrentUser.getEmail(),
      CurrentUser.getRole(),
      CurrentUser.getUserTimezone()
    );
    return new Response(Map.of("session", session));
  }

}
