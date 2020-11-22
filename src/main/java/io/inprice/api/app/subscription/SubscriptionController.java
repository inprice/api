package io.inprice.api.app.subscription;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Beans;
import io.inprice.common.models.SubsTrans;
import io.javalin.Javalin;

@Router
public class SubscriptionController implements Controller {

  private static final SubscriptionService service = Beans.getSingleton(SubscriptionService.class);
  private static final StripeService stripeService = Beans.getSingleton(StripeService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Subscription.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getCurrentCompany()));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Subscription.SAVE_INFO, (ctx) -> {
      CustomerDTO dto = ctx.bodyAsClass(CustomerDTO.class);
      ctx.json(Commons.createResponse(ctx, service.saveInfo(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.get(Consts.Paths.Subscription.TRANSACTIONS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getTransactions()));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Subscription.CREATE_SESSION + "/:plan_id", (ctx) -> {
      Integer planId = ctx.pathParam("plan_id", Integer.class).check(it -> it > 0 && it < Plans.getPlans().length).getValue();
      ctx.json(Commons.createResponse(ctx, stripeService.createCheckoutSession(planId)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Subscription.CANCEL, (ctx) -> {
      SubsTrans trans = service.getCancellationTrans();
      ctx.json(Commons.createResponse(ctx, stripeService.cancel(trans)));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Subscription.START_FREE_USE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.startFreeUse()));
    }, AccessRoles.ADMIN_ONLY());

  }

}
