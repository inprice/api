package io.inprice.scrapper.api.app.member;

import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.CompanyDTO;
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

      // resend the invitation
      app.post(Consts.Paths.Member.RESEND + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.resend(id)));
      });

      // change status
      app.put(Consts.Paths.Member.CHANGE_STATUS + "/:member_id/:status", (ctx) -> {
         Long memberId = ctx.pathParam("member_id", Long.class).check(it -> it > 0).getValue();
         MemberStatus newStatus = ctx.pathParam("status", MemberStatus.class).check(it -> it != null).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(memberId, newStatus)));
      });

      // list
      app.get(Consts.Paths.Member.BASE + "/:company_id", (ctx) -> {
         Long companyId = ctx.pathParam("company_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.getList(companyId)));
      });

   }

}
