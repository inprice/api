package io.inprice.scrapper.api.app.invitation;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.InvitationDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class InvitationController implements Controller {

   private static final InvitationService service = Beans.getSingleton(InvitationService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Invitation.SEND, (ctx) -> {
         InvitationDTO dto = ctx.bodyAsClass(InvitationDTO.class);
         ctx.json(Commons.createResponse(ctx, service.send(dto)));
      });
      app.post(Consts.Paths.Invitation.ACCEPT_NEW, (ctx) -> {
         InvitationAcceptDTO dto = ctx.bodyAsClass(InvitationAcceptDTO.class);
         ctx.json(Commons.createResponse(ctx, service.acceptNewUser(dto)));
      });

      app.put(Consts.Paths.Invitation.ACCEPT_EXISTING + "/:member_id", (ctx) -> {
         Long memberId = ctx.pathParam("member_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.acceptExisting(memberId)));
      });

      app.put(Consts.Paths.Invitation.REJECT_EXISTING + "/:member_id", (ctx) -> {
         Long memberId = ctx.pathParam("member_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.rejectExisting(memberId)));
      });

      app.post(Consts.Paths.Invitation.RESEND + "/:member_id", (ctx) -> {
         Long memberId = ctx.pathParam("member_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.resend(memberId)));
      });


   }

}
