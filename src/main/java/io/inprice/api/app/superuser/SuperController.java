package io.inprice.api.app.superuser;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SuperController extends AbstractController {

  private static final SuperService service = Beans.getSingleton(SuperService.class);

  @Override
  public void addRoutes(Javalin app) {

    // search for account
    app.post(Consts.Paths.Super.ACCOUNT, (ctx) -> {
    	try {
    		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
    		ctx.json(Commons.createResponse(ctx, service.searchAccount(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.SUPER_ONLY());

    // bind account
    app.put(Consts.Paths.Super.ACCOUNT_BIND + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.bindAccount(ctx, id)));
    }, AccessRoles.SUPER_ONLY());

    app.post(Consts.Paths.Super.ACCOUNT_UNBIND, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.unbindAccount(ctx)));
    }, AccessRoles.SUPER_ONLY());

  }

}
