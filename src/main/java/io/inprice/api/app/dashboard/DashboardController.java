package io.inprice.api.app.dashboard;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class DashboardController extends AbstractController {

  private static final DashboardService service = Beans.getSingleton(DashboardService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Dashboard.BASE, (ctx) -> {
      ctx.json(service.getReport(false));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    app.get(Consts.Paths.Dashboard.REFRESH, (ctx) -> {
      ctx.json(service.getReport(true));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

  }

}
