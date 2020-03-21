package io.inprice.scrapper.api.app.company;

import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.javalin.Javalin;

public class CompanyController {

   private static final CompanyService service = Beans.getSingleton(CompanyService.class);

   @Routing
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.REGISTER, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.register(ctx.bodyAsClass(RegisterDTO.class), ctx.ip(), ctx.userAgent())));
      });

      app.put(Consts.Paths.Company.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(CompanyDTO.class))));
      });

   }

}
