package io.inprice.scrapper.api.app.member;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.dto.MemberChangeFieldDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.consts.Consts;
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

      // change role
      app.put(Consts.Paths.Member.CHANGE_ROLE, (ctx) -> {
         MemberChangeFieldDTO dto = ctx.bodyAsClass(MemberChangeFieldDTO.class);
         dto.setStatusChange(false);
         ctx.json(Commons.createResponse(ctx, service.changeRole(dto)));
      });

      // change status
      app.put(Consts.Paths.Member.CHANGE_STATUS, (ctx) -> {
         MemberChangeFieldDTO dto = ctx.bodyAsClass(MemberChangeFieldDTO.class);
         dto.setStatusChange(true);
         ctx.json(Commons.createResponse(ctx, service.changeStatus(dto)));
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
