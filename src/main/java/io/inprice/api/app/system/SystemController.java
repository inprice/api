package io.inprice.api.app.system;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SystemController extends AbstractController {

  private static final SystemService service = Beans.getSingleton(SystemService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.System.PLANS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getPlans()));
    }, AccessRoles.ANYONE());

    app.get(Consts.Paths.System.STATISTICS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getStatistics()));
    }, AccessRoles.ANYONE());

    // this is called when subscription is completed successfully in the client side!
    app.get(Consts.Paths.System.REFRESH_SESSION, (ctx) -> {
    	ctx.json(Commons.createResponse(ctx, service.refreshSession()));
    }, AccessRoles.ADMIN());

  }

}
