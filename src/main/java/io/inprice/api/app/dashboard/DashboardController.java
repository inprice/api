package io.inprice.api.app.dashboard;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class DashboardController implements Controller {

  private static final DashboardService service = Beans.getSingleton(DashboardService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Dashboard.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getReport(false)));
    }, AccessRoles.ANYONE());

    app.get(Consts.Paths.Dashboard.REFRESH, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getReport(true)));
    }, AccessRoles.ANYONE());

  }

}
