package io.inprice.api.app.superuser.user;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class Controller extends AbstractController {

  private static final Service service = Beans.getSingleton(Service.class);

  @Override
  public void addRoutes(Javalin app) {

    // search
    app.post(Consts.Paths.Super.User._BASE, (ctx) -> {
  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.SUPER_ONLY());

    // fetch details
    app.get(Consts.Paths.Super.User._BASE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		ctx.json(Commons.createResponse(ctx, service.fetchDetails(id)));
    }, AccessRoles.SUPER_ONLY());

    // ban
    app.post(Consts.Paths.Super.User.BAN, (ctx) -> {
  		IdTextDTO dto = ctx.bodyAsClass(IdTextDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.ban(dto)));
    }, AccessRoles.SUPER_ONLY());

    // revoke ban
    app.put(Consts.Paths.Super.User.REVOKE_BAN + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.revokeBan(id)));
    }, AccessRoles.SUPER_ONLY());

  }

}
