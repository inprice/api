package io.inprice.api.app.auth;

import io.inprice.api.app.auth.dto.EmailDTO;
import io.inprice.api.app.auth.dto.InvitationAcceptDTO;
import io.inprice.api.app.user.dto.LoginDTO;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.models.User;
import io.javalin.Javalin;

@Router
public class AuthController extends AbstractController {

  private static final AuthService service = Beans.getSingleton(AuthService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Auth.LOGIN, (ctx) -> {
    	try {
        LoginDTO dto = ctx.bodyAsClass(LoginDTO.class);
        ctx.json(Commons.createResponse(ctx, service.login(ctx, dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    });

    app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
    	try {
        EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
        ctx.json(Commons.createResponse(ctx, service.forgotPassword(dto.getEmail())));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    });

    app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
    	try {
        PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
        ctx.json(Commons.createResponse(ctx, service.resetPassword(ctx, dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    });

    app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.logout(ctx)));
    });

    app.post(Consts.Paths.Auth.ACCEPT_INVITATION, (ctx) -> {
      String timezone = ClientSide.getGeoInfo(ctx.req).get(Consts.TIMEZONE);

      InvitationAcceptDTO dto = ctx.bodyAsClass(InvitationAcceptDTO.class);
      Response res = service.acceptNewUser(dto, timezone);
      if (res.isOK()) {
        User user = res.getData();
        ctx.json(Commons.createResponse(ctx, service.createSession(ctx, user)));
      } else {
        ctx.json(Commons.createResponse(ctx, res));
      }
    });

  }

}
