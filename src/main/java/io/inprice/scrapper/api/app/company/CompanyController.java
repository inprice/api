package io.inprice.scrapper.api.app.company;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.consts.Consts;
import io.javalin.Javalin;

@Router
public class CompanyController implements Controller {

   private static final CompanyService service = Beans.getSingleton(CompanyService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.REGISTER_REQUEST, (ctx) -> {
         RegisterDTO dto = ctx.bodyAsClass(RegisterDTO.class);
         ctx.json(Commons.createResponse(ctx, service.registerRequest(dto, ctx.ip())));
      });

      app.get(Consts.Paths.Auth.REGISTER + "/:token", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.register(ctx.pathParam("token"), ctx.ip())));
      });

      app.put(Consts.Paths.Company.BASE, (ctx) -> {
         CompanyDTO dto = ctx.bodyAsClass(CompanyDTO.class);
         ctx.json(Commons.createResponse(ctx, service.update(dto)));
      });

   }

}
