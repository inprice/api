package io.inprice.scrapper.api.app.auth;

import io.inprice.scrapper.api.app.membership.MembershipService;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.ClientSide;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.models.User;
import io.javalin.Javalin;

@Router
public class AuthController implements Controller {

  private static final AuthService authService = Beans.getSingleton(AuthService.class);
  private static final MembershipService membershipService = Beans.getSingleton(MembershipService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Auth.LOGIN, (ctx) -> {
      LoginDTO dto = ctx.bodyAsClass(LoginDTO.class);
      ctx.json(Commons.createResponse(ctx, authService.login(ctx, dto)));
    });

    app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
      EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
      ctx.json(Commons.createResponse(ctx, authService.forgotPassword(dto.getEmail())));
    });

    app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
      PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
      ctx.json(Commons.createResponse(ctx, authService.resetPassword(ctx, dto)));
    });

    app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, authService.logout(ctx)));
    });

    app.post(Consts.Paths.Auth.ACCEPT_INVITATION, (ctx) -> {
      String timezone = ClientSide.getGeoInfo(ctx.req).get(Consts.TIMEZONE);

      InvitationAcceptDTO dto = ctx.bodyAsClass(InvitationAcceptDTO.class);
      ServiceResponse res = membershipService.acceptNewUser(dto, timezone);
      if (res.isOK()) {
        User user = res.getData();
        ctx.json(Commons.createResponse(ctx, authService.createSession(ctx, user)));
      } else {
        ctx.json(Commons.createResponse(ctx, res));
      }
    });

  }

}
