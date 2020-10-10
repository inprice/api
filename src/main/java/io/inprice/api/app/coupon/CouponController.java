package io.inprice.api.app.coupon;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class CouponController implements Controller {

  private static final CouponService service = Beans.getSingleton(CouponService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Coupon.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getCoupons()));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Coupon.APPLY + "/:code", (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.applyCoupon(ctx.pathParam("code"))));
    }, AccessRoles.ADMIN_ONLY());

  }

}