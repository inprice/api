package io.inprice.api.app.auth;

import io.inprice.api.app.auth.dto.InvitationAcceptDTO;
import io.inprice.api.app.auth.dto.LoginDTO;
import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.membership.MembershipService;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.EmailDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.models.User;
import io.javalin.Javalin;

@Router
public class AuthController implements Controller {

  private static final AuthService service = Beans.getSingleton(AuthService.class);
  private static final MembershipService membershipService = Beans.getSingleton(MembershipService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Auth.LOGIN, (ctx) -> {
      LoginDTO dto = ctx.bodyAsClass(LoginDTO.class);
      ctx.json(Commons.createResponse(ctx, service.login(ctx, dto)));
    });

    app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
      EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
      ctx.json(Commons.createResponse(ctx, service.forgotPassword(dto.getEmail())));
    });

    app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
      PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
      ctx.json(Commons.createResponse(ctx, service.resetPassword(ctx, dto)));
    });

    app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.logout(ctx)));
    });

    app.post(Consts.Paths.Auth.ACCEPT_INVITATION, (ctx) -> {
      String timezone = ClientSide.getGeoInfo(ctx.req).get(Consts.TIMEZONE);

      InvitationAcceptDTO dto = ctx.bodyAsClass(InvitationAcceptDTO.class);
      Response res = membershipService.acceptNewUser(dto, timezone);
      if (res.isOK()) {
        User user = res.getData();
        ctx.json(Commons.createResponse(ctx, service.createSession(ctx, user)));
      } else {
        ctx.json(Commons.createResponse(ctx, res));
      }
    });

  }

}
