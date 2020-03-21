package io.inprice.scrapper.api.app.company;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.rest.component.Commons;
import io.javalin.Javalin;

public class CompanyController {

   private static final CompanyService service = Beans.getSingleton(CompanyService.class);

   @Routing
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.REGISTER, (ctx) -> {
         String ip = ctx.ip();
         String userAgent = ctx.userAgent();
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(CompanyDTO.class), ip, userAgent)));
      });

      app.put(Consts.Paths.Company.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(CompanyDTO.class))));
      });

   }

}
