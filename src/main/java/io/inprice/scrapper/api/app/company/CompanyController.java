package io.inprice.scrapper.api.app.company;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class CompanyController implements Controller {

   private static final CompanyService service = Beans.getSingleton(CompanyService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.REQUEST_REGISTRATION, (ctx) -> {
         RegisterDTO dto = ctx.bodyAsClass(RegisterDTO.class);
         ctx.json(Commons.createResponse(ctx, service.requestRegistration(dto, ctx.ip())));
      });

      app.post(Consts.Paths.Auth.COMPLETE_REGISTRATION, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.completeRegistration(ctx, ctx.queryParam("token"))));
      });

      app.get(Consts.Paths.Company.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getCurrentCompany()));
      }, AccessRoles.ANYONE());

      app.post(Consts.Paths.Company.BASE, (ctx) -> {
         CompanyDTO dto = ctx.bodyAsClass(CompanyDTO.class);
         ctx.json(Commons.createResponse(ctx, service.create(dto)));
      }, AccessRoles.ANYONE());

      app.put(Consts.Paths.Company.BASE, (ctx) -> {
         CompanyDTO dto = ctx.bodyAsClass(CompanyDTO.class);
         ctx.json(Commons.createResponse(ctx, service.update(dto)));
      }, AccessRoles.ADMIN_ONLY());

   }

}
