package io.inprice.scrapper.api.app.member;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class MemberController implements Controller {

   private static final MemberService service = Beans.getSingleton(MemberService.class);

   @Override
   public void addRoutes(Javalin app) {

      // send a new invitation
      app.post(Consts.Paths.Member.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.sendInvitation(ctx.bodyAsClass(MemberDTO.class))));
      });

      // handles confirmed and rejected invitations
      app.get(Consts.Paths.Auth.INVITATION, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.handleInvitation(ctx.queryParam("token"), ctx.ip())));
      });

      // toggle active status
      app.put(Consts.Paths.Member.TOGGLE_STATUS + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.toggleStatus(id)));
      });

      // change role
      app.put(Consts.Paths.Member.CHANGE_ROLE, (ctx) -> {
         MemberChangeRoleDTO dto = ctx.bodyAsClass(MemberChangeRoleDTO.class);
         ctx.json(Commons.createResponse(ctx, service.changeRole(dto)));
      });

      // list
      app.get(Consts.Paths.Member.BASE + "s", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList()));
      });

      // resend the invitation
      app.post(Consts.Paths.Member.RESEND + "/:member_id", (ctx) -> {
         Long memberId = ctx.pathParam("member_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.resend(memberId)));
      });

   }

}
