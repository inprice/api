package io.inprice.api.app.account;

import java.util.Map;

import io.inprice.api.app.auth.AuthService;
import io.inprice.api.app.account.dto.CreateDTO;
import io.inprice.api.app.account.dto.RegisterDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.StringDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class AccountController implements Controller {

  private final AccountService service = Beans.getSingleton(AccountService.class);
  private final AuthService authService = Beans.getSingleton(AuthService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Auth.REQUEST_REGISTRATION, (ctx) -> {
    	try {
        RegisterDTO dto = ctx.bodyAsClass(RegisterDTO.class);
        ctx.json(Commons.createResponse(ctx, service.requestRegistration(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    });

    app.post(Consts.Paths.Auth.COMPLETE_REGISTRATION, (ctx) -> {
      Response res = service.completeRegistration(ctx, ctx.queryParam("token"));
      if (res.isOK()) {
        res = authService.createSession(ctx, res.getData());
      }
      ctx.json(Commons.createResponse(ctx, res));
    });

    app.get(Consts.Paths.Account.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getCurrentAccount()));
    }, AccessRoles.ANYONE());

    app.get(Consts.Paths.Account.GEO_INFO, (ctx) -> {
      Map<String, String> map = ClientSide.getGeoInfo(ctx.req);
      ctx.json(Commons.createResponse(ctx, new Response(map)));
    }, AccessRoles.ANYONE());

    // create
    app.post(Consts.Paths.Account.BASE, (ctx) -> {
    	try {
        CreateDTO dto = ctx.bodyAsClass(CreateDTO.class);
        ctx.json(Commons.createResponse(ctx, service.create(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    }, AccessRoles.ANYONE());

    // update
    app.put(Consts.Paths.Account.BASE, (ctx) -> {
    	try {
        CreateDTO dto = ctx.bodyAsClass(CreateDTO.class);
        ctx.json(Commons.createResponse(ctx, service.update(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Account.DELETE, (ctx) -> {
      StringDTO dto = ctx.bodyAsClass(StringDTO.class);
      ctx.json(Commons.createResponse(ctx, service.deleteAccount(dto.getValue())));
    }, AccessRoles.ADMIN_ONLY());

  }

}
