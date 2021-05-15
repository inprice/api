package io.inprice.api.app.member;

import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.member.dto.InvitationUpdateDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class MemberController extends AbstractController {

  private static final MemberService service = Beans.getSingleton(MemberService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Member.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getList()));
    }, AccessRoles.ADMIN_OR_SUPER());

    app.post(Consts.Paths.Member.BASE, (ctx) -> {
    	try {
        InvitationSendDTO dto = ctx.bodyAsClass(InvitationSendDTO.class);
        ctx.json(Commons.createResponse(ctx, service.invite(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.ADMIN());

    app.post(Consts.Paths.Member.BASE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resend(memId)));
    }, AccessRoles.ADMIN());

    app.delete(Consts.Paths.Member.DELETE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(memId)));
    }, AccessRoles.ADMIN());

    app.put(Consts.Paths.Member.CHANGE_ROLE, (ctx) -> {
    	try {
        InvitationUpdateDTO dto = ctx.bodyAsClass(InvitationUpdateDTO.class);
        ctx.json(Commons.createResponse(ctx, service.changeRole(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.ADMIN());

    app.put(Consts.Paths.Member.PAUSE + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.pause(memId)));
    }, AccessRoles.ADMIN());

    app.put(Consts.Paths.Member.RESUME + "/:mem_id", (ctx) -> {
      Long memId = ctx.pathParam("mem_id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.resume(memId)));
    }, AccessRoles.ADMIN());

  }

}
