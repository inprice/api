package io.inprice.api.app.billing;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class BillingController implements Controller {

  private static final BillingService service = Beans.getSingleton(BillingService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Billing.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getInfo()));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Billing.SAVE_INFO, (ctx) -> {
      CustomerInfoDTO dto = ctx.bodyAsClass(CustomerInfoDTO.class);
      ctx.json(Commons.createResponse(ctx, service.saveInfo(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.get(Consts.Paths.Billing.TRANSACTIONS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getTransactions()));
    }, AccessRoles.ADMIN_ONLY());

  }

}
