package io.inprice.api.app.subscription;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SubscriptionController extends AbstractController {

  private static final SubscriptionService service = Beans.getSingleton(SubscriptionService.class);

  @Override
  public void addRoutes(Javalin app) {

    // creates checkout session
    app.post(Consts.Paths.Subscription.CREATE_CHECKOUT + "/:plan_id", (ctx) -> {
      Integer planId = ctx.pathParam("plan_id", Integer.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.createCheckout(planId)));
    }, AccessRoles.ADMIN());

    // cancels a subscription
    app.put(Consts.Paths.Subscription.CANCEL, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.cancel()));
    }, AccessRoles.ADMIN());

    // starts free use
    app.post(Consts.Paths.Subscription.START_FREE_USE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.startFreeUse()));
    }, AccessRoles.ADMIN());

    // updates account's extra info used in invoices
    app.post(Consts.Paths.Subscription.SAVE_INFO, (ctx) -> {
      CustomerDTO dto = ctx.bodyAsClass(CustomerDTO.class);
      ctx.json(Commons.createResponse(ctx, service.saveInfo(dto)));
    }, AccessRoles.ADMIN());

    // returns all the transactions happened in payment provider
    app.get(Consts.Paths.Subscription.GET_INFO, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getInfo()));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

  }

}
