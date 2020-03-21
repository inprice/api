package io.inprice.scrapper.api.app.company;

import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.javalin.Javalin;

public class CompanyController {

   private static final CompanyService service = Beans.getSingleton(CompanyService.class);

   @Routing
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.REGISTER_REQUEST, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.registerRequest(ctx.bodyAsClass(RegisterDTO.class), ctx.ip())));
      });

      app.post(Consts.Paths.Auth.REGISTER, (ctx) -> {
         String registerRequestToken = ctx.body();
         ctx.json(Commons.createResponse(ctx, service.register(registerRequestToken, ctx.ip())));
      });

   }

}
