package io.inprice.scrapper.api.app.auth;

import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.javalin.Javalin;

public class AuthController {

   private static final AuthService service = Beans.getSingleton(AuthService.class);

   @Routing
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.LOGIN, (ctx) -> {
         LoginDTO loginDTO = ctx.bodyAsClass(LoginDTO.class);
         loginDTO.setIp(ctx.ip());
         loginDTO.setUserAgent(ctx.userAgent());
         ctx.json(Commons.createResponse(ctx, service.login(loginDTO)));
      });

      app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.forgotPassword(ctx.bodyAsClass(EmailDTO.class), ctx.ip())));
      });

      app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.resetPassword(ctx.bodyAsClass(PasswordDTO.class))));
      });

      app.post(Consts.Paths.Auth.REFRESH_TOKEN, (ctx) -> {
         String token = ctx.body();
         String ip = ctx.ip();
         String userAgent = ctx.userAgent();
         ServiceResponse serres = service.refreshTokens(token, ip, userAgent);
         if (serres.isOK())
            ctx.json(Commons.createResponse(ctx, serres));
         else
            ctx.status(401);
      });

      app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
         String authHeader = ctx.header(Consts.Auth.AUTHORIZATION_HEADER);
         ctx.json(Commons.createResponse(ctx, service.logout(authHeader)));
      });

   }

}
