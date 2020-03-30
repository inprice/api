package io.inprice.scrapper.api.app.auth;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.javalin.Javalin;

@Router
public class AuthController implements Controller {

   private static final AuthService service = Beans.getSingleton(AuthService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.post(Consts.Paths.Auth.LOGIN, (ctx) -> {
         LoginDTO loginDTO = ctx.bodyAsClass(LoginDTO.class);
         loginDTO.setIp(ctx.ip());
         loginDTO.setUserAgent(ctx.userAgent());
         ctx.json(Commons.createResponse(ctx, service.login(loginDTO)));
      });

      app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
         EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
         ctx.json(Commons.createResponse(ctx, service.forgotPassword(dto.getEmail(), ctx.ip())));
      });

      app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.resetPassword(ctx.bodyAsClass(PasswordDTO.class))));
      });

      app.post(Consts.Paths.Auth.REFRESH_TOKEN, (ctx) -> {
         String refreshToken = ctx.body();
         String accessToken = ctx.header(Consts.Auth.AUTHORIZATION_HEADER);

         ServiceResponse serres = service.refreshTokens(refreshToken, accessToken, ctx.ip(), ctx.userAgent());
         if (serres.isOK())
            ctx.json(Commons.createResponse(ctx, serres));
         else
            ctx.status(HttpStatus.UNAUTHORIZED_401);
      });

      app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
         String refreshToken = ctx.body();
         String accessToken = ctx.header(Consts.Auth.AUTHORIZATION_HEADER);
         ctx.json(Commons.createResponse(ctx, service.logout(refreshToken, accessToken)));
      });

   }

}
