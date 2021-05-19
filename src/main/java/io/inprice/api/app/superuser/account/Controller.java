package io.inprice.api.app.superuser.account;

import io.inprice.api.app.superuser.account.dto.CreateCouponDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
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
    app.post(Consts.Paths.Super.Account._BASE, (ctx) -> {
  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.SUPER_ONLY());

    // bind
    app.put(Consts.Paths.Super.Account.BIND + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.bind(ctx, id)));
    }, AccessRoles.SUPER_ONLY());
    
    // unbind
    app.post(Consts.Paths.Super.Account.UNBIND, (ctx) -> {
    	ctx.json(Commons.createResponse(ctx, service.unbind(ctx)));
    }, AccessRoles.SUPER_ONLY());

    // create coupon
    app.post(Consts.Paths.Super.Account.COUPON, (ctx) -> {
  		CreateCouponDTO dto = ctx.bodyAsClass(CreateCouponDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.createCoupon(dto)));
    }, AccessRoles.SUPER_ONLY());

  }

}
