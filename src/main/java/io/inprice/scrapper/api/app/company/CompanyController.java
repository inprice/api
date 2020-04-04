package io.inprice.scrapper.api.app.company;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;

import org.eclipse.jetty.http.HttpStatus;

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

      app.get(Consts.Paths.Auth.REGISTER, (ctx) -> {
         ctx.status(HttpStatus.MOVED_TEMPORARILY_302);
         ServiceResponse res = service.register(ctx.queryParam("token"), ctx.ip());
         if (res.isOK()) {
            ctx.redirect(Props.getWebUrl() + Consts.Paths.Auth.LOGIN + "?m=ax37");
         } else {
            ctx.redirect(Props.getWebUrl() + Consts.Paths.Auth.LOGIN + "?m=qb41");
         }
      });

      app.put(Consts.Paths.Company.BASE, (ctx) -> {
         CompanyDTO dto = ctx.bodyAsClass(CompanyDTO.class);
         ctx.json(Commons.createResponse(ctx, service.update(dto)));
      });

   }

}
