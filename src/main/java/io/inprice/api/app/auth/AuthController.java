package io.inprice.api.app.auth;

import io.inprice.api.app.auth.dto.EmailDTO;
import io.inprice.api.app.auth.dto.InvitationAcceptDTO;
import io.inprice.api.app.user.dto.LoginDTO;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.ClientSide;
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
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      LoginDTO dto = ctx.bodyAsClass(LoginDTO.class);
	      ctx.json(service.login(ctx, dto));
    	}
    });

    app.post(Consts.Paths.Auth.FORGOT_PASSWORD, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      EmailDTO dto = ctx.bodyAsClass(EmailDTO.class);
	      ctx.json(service.forgotPassword(dto.getEmail()));
    	}
    });

    app.post(Consts.Paths.Auth.RESET_PASSWORD, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
	      ctx.json(service.resetPassword(ctx, dto));
    	}
    });

    app.post(Consts.Paths.Auth.LOGOUT, (ctx) -> {
      ctx.json(service.logout(ctx));
    });

    app.post(Consts.Paths.Auth.ACCEPT_INVITATION, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      String timezone = ClientSide.getGeoInfo(ctx.req).get(Consts.TIMEZONE);
	
	      InvitationAcceptDTO dto = ctx.bodyAsClass(InvitationAcceptDTO.class);
	      Response res = service.acceptNewUser(dto, timezone);
	      if (res.isOK()) {
	        User user = res.getData();
	        res = service.createSession(ctx, user);
	      }
	    	ctx.json(res);
    	}
    });

  }

}
