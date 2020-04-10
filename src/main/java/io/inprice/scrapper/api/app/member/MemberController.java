package io.inprice.scrapper.api.app.member;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
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

      app.delete(Consts.Paths.Member.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id, ctx.ip(), ctx.userAgent())));
      });

      app.put(Consts.Paths.Member.TOGGLE_STATUS + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.toggleStatus(id, ctx.ip(), ctx.userAgent())));
      });

      app.put(Consts.Paths.Member.CHANGE_ROLE, (ctx) -> {
         MemberChangeRoleDTO dto = ctx.bodyAsClass(MemberChangeRoleDTO.class);
         ctx.json(Commons.createResponse(ctx, service.changeRole(dto, ctx.ip(), ctx.userAgent())));
      });

      app.get(Consts.Paths.Member.BASE + "s", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList()));
      });

   }

}
