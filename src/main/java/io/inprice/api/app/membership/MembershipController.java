package io.inprice.api.app.membership;

import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.membership.dto.InvitationUpdateDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class MembershipController extends AbstractController {

  private static final MembershipService service = Beans.getSingleton(MembershipService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Membership.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getList()));
    }, AccessRoles.ADMIN_OR_SUPER());

    app.post(Consts.Paths.Membership.BASE, (ctx) -> {
      InvitationSendDTO dto = ctx.bodyAsClass(InvitationSendDTO.class);
      ctx.json(Commons.createResponse(ctx, service.invite(dto)));
    }, AccessRoles.ADMIN());

    app.post(Consts.Paths.Membership.BASE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resend(memId)));
    }, AccessRoles.ADMIN());

    app.delete(Consts.Paths.Membership.BASE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(memId)));
    }, AccessRoles.ADMIN());

    app.put(Consts.Paths.Membership.CHANGE_ROLE, (ctx) -> {
      InvitationUpdateDTO dto = ctx.bodyAsClass(InvitationUpdateDTO.class);
      ctx.json(Commons.createResponse(ctx, service.changeRole(dto)));
    }, AccessRoles.ADMIN());

    app.put(Consts.Paths.Membership.PAUSE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.pause(memId)));
    }, AccessRoles.ADMIN());

    app.put(Consts.Paths.Membership.RESUME + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resume(memId)));
    }, AccessRoles.ADMIN());

  }

}
