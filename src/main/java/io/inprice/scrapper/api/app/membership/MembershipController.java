package io.inprice.scrapper.api.app.membership;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.InvitationSendDTO;
import io.inprice.scrapper.api.dto.InvitationUpdateDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class MembershipController implements Controller {

  private static final MembershipService service = Beans.getSingleton(MembershipService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Membership.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getList()));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Membership.BASE, (ctx) -> {
      InvitationSendDTO dto = ctx.bodyAsClass(InvitationSendDTO.class);
      ctx.json(Commons.createResponse(ctx, service.invite(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Membership.BASE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resend(memId)));
    }, AccessRoles.ADMIN_ONLY());

    app.delete(Consts.Paths.Membership.DELETE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(memId)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Membership.CHANGE_ROLE, (ctx) -> {
      InvitationUpdateDTO dto = ctx.bodyAsClass(InvitationUpdateDTO.class);
      ctx.json(Commons.createResponse(ctx, service.changeRole(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Membership.PAUSE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.pause(memId)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Membership.RESUME + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resume(memId)));
    }, AccessRoles.ADMIN_ONLY());

  }

}
