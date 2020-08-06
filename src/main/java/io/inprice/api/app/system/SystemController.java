package io.inprice.api.app.system;

import io.inprice.api.app.plan.PlanRepository;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SystemController implements Controller {

  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.System.PLANS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, planRepository.getPlans()));
    }, AccessRoles.ANYONE());

  }

}
