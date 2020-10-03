package io.inprice.api.app.system;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class SystemController implements Controller {

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.System.PLANS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, PlanDao.getPlans()));
    }, AccessRoles.ANYONE());

  }

}
