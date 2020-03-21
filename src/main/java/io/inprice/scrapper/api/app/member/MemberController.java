package io.inprice.scrapper.api.app.member;

import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.MemberChangeFieldDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.javalin.Javalin;

public class MemberController {

   private static final MemberService service = Beans.getSingleton(MemberService.class);

   @Routing
   public void addRoutes(Javalin app) {

      // add a new invitation
      app.post(Consts.Paths.Member.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.invite(ctx.bodyAsClass(MemberDTO.class))));
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
