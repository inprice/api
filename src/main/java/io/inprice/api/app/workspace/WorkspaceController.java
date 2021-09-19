package io.inprice.api.app.workspace;

import java.util.Map;

import io.inprice.api.app.workspace.dto.CreateDTO;
import io.inprice.api.app.workspace.dto.RegisterDTO;
import io.inprice.api.app.auth.AuthService;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.StringDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class WorkspaceController extends AbstractController {

  private final WorkspaceService service = Beans.getSingleton(WorkspaceService.class);
  private final AuthService authService = Beans.getSingleton(AuthService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Auth.REQUEST_REGISTRATION, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      RegisterDTO dto = ctx.bodyAsClass(RegisterDTO.class);
	      ctx.json(service.requestRegistration(dto));
    	}
    });

    app.post(Consts.Paths.Auth.COMPLETE_REGISTRATION, (ctx) -> {
      Response res = service.completeRegistration(ctx, ctx.queryParam("token"));
      if (res.isOK()) {
        res = authService.createSession(ctx, res.getData());
      }
      ctx.json(res);
    });

    // find
    app.get(Consts.Paths.Workspace.BASE, (ctx) -> {
      ctx.json(service.getCurrentWorkspace());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    app.get(Consts.Paths.Workspace.GEO_INFO, (ctx) -> {
      Map<String, String> map = ClientSide.getGeoInfo(ctx.req);
      ctx.json(new Response(map));
    }, AccessRoles.ANYONE());

    // create
    app.post(Consts.Paths.Workspace.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      CreateDTO dto = ctx.bodyAsClass(CreateDTO.class);
	      ctx.json(service.create(dto));
    	}
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // update
    app.put(Consts.Paths.Workspace.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      CreateDTO dto = ctx.bodyAsClass(CreateDTO.class);
	      ctx.json(service.update(dto));
    	}
    }, AccessRoles.ADMIN());

    //delete
    app.delete(Consts.Paths.Workspace.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	      StringDTO dto = ctx.bodyAsClass(StringDTO.class);
	      ctx.json(service.deleteWorkspace(dto.getValue()));
    	}
    }, AccessRoles.ADMIN());

  }

}
