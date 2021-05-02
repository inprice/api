package io.inprice.api.app.subscription;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SubscriptionController implements Controller {

  private static final SubscriptionService service = Beans.getSingleton(SubscriptionService.class);

  @Override
  public void addRoutes(Javalin app) {

    // creates checkout session for stripe
    app.post(Consts.Paths.Subscription.CREATE_CHECKOUT + "/:plan_id", (ctx) -> {
      Integer planId = ctx.pathParam("plan_id", Integer.class).check(it -> it > 0 && it < Plans.getPlans().length).getValue();
      ctx.json(Commons.createResponse(ctx, service.createCheckout(planId)));
    }, AccessRoles.ADMIN_ONLY());

    // cancels a subscription
    app.put(Consts.Paths.Subscription.CANCEL, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.cancel()));
    }, AccessRoles.ADMIN_ONLY());

    // starts free use
    app.post(Consts.Paths.Subscription.START_FREE_USE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.startFreeUse()));
    }, AccessRoles.ADMIN_ONLY());

    // returns current account's info
    app.get(Consts.Paths.Subscription.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getCurrentAccount()));
    }, AccessRoles.ANYONE());

    // returns all the transactions happened in payment provider
    app.get(Consts.Paths.Subscription.TRANSACTIONS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getTransactions()));
    }, AccessRoles.ANYONE());

    // updates account's extra info used in invoices
    app.post(Consts.Paths.Subscription.SAVE_INFO, (ctx) -> {
    	try {
        CustomerDTO dto = ctx.bodyAsClass(CustomerDTO.class);
        ctx.json(Commons.createResponse(ctx, service.saveInfo(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    }, AccessRoles.ADMIN_ONLY());

  }

}
