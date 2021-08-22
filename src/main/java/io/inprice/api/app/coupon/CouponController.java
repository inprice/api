package io.inprice.api.app.coupon;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class CouponController extends AbstractController {

  private static final CouponService service = Beans.getSingleton(CouponService.class);

  @Override
  public void addRoutes(Javalin app) {

    // returns coupons managed by current account
    app.get(Consts.Paths.Coupon.BASE, (ctx) -> {
      ctx.json(service.getCoupons());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // registers the given coupon to current account
    app.put(Consts.Paths.Coupon.APPLY + "/:code", (ctx) -> {
      ctx.json(service.applyCoupon(ctx.pathParam("code")));
    }, AccessRoles.ADMIN());

  }

}
