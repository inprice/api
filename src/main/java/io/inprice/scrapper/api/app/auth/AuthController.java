package io.inprice.scrapper.api.app.auth;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class AuthController implements Controller {

   private static final AuthService service = Beans.getSingleton(AuthService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.LOGIN, (ctx) -> {
         LoginDTO dto = ctx.bodyAsClass(LoginDTO.class);
         ctx.json(Commons.createResponse(ctx, service.login(ctx, dto)));
      });

      app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
         EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
         ctx.json(Commons.createResponse(ctx, service.forgotPassword(dto.getEmail(), ctx.ip())));
      });

      app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
         PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
         ctx.json(Commons.createResponse(ctx, service.resetPassword(ctx, dto)));
      });

      app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.logout(ctx)));
      });

   }

}
