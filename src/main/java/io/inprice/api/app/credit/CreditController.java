package io.inprice.api.app.credit;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class CreditController extends AbstractController {

  private static final CreditService service = Beans.getSingleton(CreditService.class);

  @Override
  public void addRoutes(Javalin app) {

    // returns credits managed by current workspace
    app.get(Consts.Paths.Credit.BASE, (ctx) -> {
      ctx.json(service.getCredits());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // registers the given credit to current workspace
    app.put(Consts.Paths.Credit.APPLY + "/:code", (ctx) -> {
      ctx.json(service.applyCredit(ctx.pathParam("code")));
    }, AccessRoles.ADMIN());

  }

}
