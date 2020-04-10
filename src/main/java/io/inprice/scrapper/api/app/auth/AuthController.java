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
         dto.setIp(ctx.ip());
         dto.setUserAgent(ctx.userAgent());
         ctx.json(Commons.createResponse(ctx, service.login(dto)));
      });

      app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
         EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
         ctx.json(Commons.createResponse(ctx, service.forgotPassword(dto.getEmail(), ctx.ip())));
      });

      app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
         PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
         ctx.json(Commons.createResponse(ctx, service.resetPassword(dto, ctx.ip(), ctx.userAgent())));
      });

      app.post(Consts.Paths.Auth.REFRESH_TOKEN, (ctx) -> {
         String refreshToken = ctx.header(Consts.AUTHORIZATION_HEADER);
         ctx.json(Commons.createResponse(ctx, service.refreshTokens(refreshToken, ctx.ip(), ctx.userAgent())));
      });

      app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
         EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
         ctx.json(Commons.createResponse(ctx, service.logout(dto, ctx.ip(), ctx.userAgent())));
      });

   }

}
