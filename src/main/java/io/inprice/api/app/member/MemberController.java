package io.inprice.api.app.member;

import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.member.dto.InvitationUpdateDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class MemberController implements Controller {

  private static final MemberService service = Beans.getSingleton(MemberService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Member.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getList()));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Member.BASE, (ctx) -> {
      InvitationSendDTO dto = ctx.bodyAsClass(InvitationSendDTO.class);
      ctx.json(Commons.createResponse(ctx, service.invite(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.post(Consts.Paths.Member.BASE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resend(memId)));
    }, AccessRoles.ADMIN_ONLY());

    app.delete(Consts.Paths.Member.DELETE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(memId)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Member.CHANGE_ROLE, (ctx) -> {
      InvitationUpdateDTO dto = ctx.bodyAsClass(InvitationUpdateDTO.class);
      ctx.json(Commons.createResponse(ctx, service.changeRole(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Member.PAUSE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.pause(memId)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Member.RESUME + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resume(memId)));
    }, AccessRoles.ADMIN_ONLY());

  }

}
