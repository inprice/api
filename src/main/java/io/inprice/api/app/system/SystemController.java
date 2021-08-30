package io.inprice.api.app.system;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.SqlHelper;
import io.javalin.Javalin;

@Router
public class SystemController extends AbstractController {

  private static final SystemService service = Beans.getSingleton(SystemService.class);

  @Override
  public void addRoutes(Javalin app) {

		app.get(Consts.Paths.System.SEARCH, (ctx) -> {
    	String term = ctx.queryParam("term", String.class).check(it -> StringUtils.isNotBlank(it)).getValue();
    	ctx.json(service.search(SqlHelper.clear(term)));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    app.get(Consts.Paths.System.PLANS, (ctx) -> {
      ctx.json(service.getPlans());
    }, AccessRoles.ANYONE());

    app.get(Consts.Paths.System.STATISTICS, (ctx) -> {
      ctx.json(service.getStatistics());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // this is called when subscription is completed successfully in the client side!
    app.get(Consts.Paths.System.REFRESH_SESSION, (ctx) -> {
    	ctx.json(service.refreshSession());
    }, AccessRoles.ADMIN());

  }

}
