package io.inprice.api.app.subscription;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SubscriptionController implements Controller {

  private static final SubscriptionService service = Beans.getSingleton(SubscriptionService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Subscription.TRANSACTIONS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getTransactions()));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Subscription.CANCEL, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.cancel()));
    }, AccessRoles.ADMIN_ONLY());

  }

}
