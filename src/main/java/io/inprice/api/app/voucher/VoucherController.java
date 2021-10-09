package io.inprice.api.app.voucher;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class VoucherController extends AbstractController {

  private static final VoucherService service = Beans.getSingleton(VoucherService.class);

  @Override
  public void addRoutes(Javalin app) {

    // returns vouchers managed by current workspace
    app.get(Consts.Paths.Voucher.BASE, (ctx) -> {
      ctx.json(service.getVouchers());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // registers the given voucher to current workspace
    app.put(Consts.Paths.Voucher.APPLY + "/:code", (ctx) -> {
      ctx.json(service.applyVoucher(ctx.pathParam("code")));
    }, AccessRoles.ADMIN());

  }

}
