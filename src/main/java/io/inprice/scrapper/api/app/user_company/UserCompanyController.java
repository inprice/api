package io.inprice.scrapper.api.app.user_company;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class UserCompanyController implements Controller {

   private static final UserCompanyService service = Beans.getSingleton(UserCompanyService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.delete(Consts.Paths.UserCompany.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      }, AccessRoles.ADMIN_ONLY());

      app.put(Consts.Paths.UserCompany.CHANGE_ROLE, (ctx) -> {
         MemberChangeRoleDTO dto = ctx.bodyAsClass(MemberChangeRoleDTO.class);
         ctx.json(Commons.createResponse(ctx, service.changeRole(dto, ctx.ip(), ctx.userAgent())));
      }, AccessRoles.ADMIN_ONLY());

      app.get(Consts.Paths.UserCompany.BASE + "s", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList()));
      }, AccessRoles.ADMIN_ONLY());

   }

}
