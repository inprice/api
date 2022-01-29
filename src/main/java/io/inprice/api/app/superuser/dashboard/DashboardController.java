package io.inprice.api.app.superuser.dashboard;

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

    app.get(Consts.Paths.Super.Dashboard._BASE, (ctx) -> {
      ctx.json(service.getReport(false));
    }, AccessRoles.SUPER_ONLY());

    app.get(Consts.Paths.Super.Dashboard.REFRESH, (ctx) -> {
      ctx.json(service.getReport(true));
    }, AccessRoles.SUPER_ONLY());
    
  }

}
