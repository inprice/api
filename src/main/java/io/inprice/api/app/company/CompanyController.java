package io.inprice.api.app.company;

import java.util.Map;

import io.inprice.api.app.auth.AuthService;
import io.inprice.api.app.company.dto.CreateDTO;
import io.inprice.api.app.company.dto.RegisterDTO;
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
public class CompanyController implements Controller {

  private final CompanyService service = Beans.getSingleton(CompanyService.class);
  private final AuthService authService = Beans.getSingleton(AuthService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Auth.REQUEST_REGISTRATION, (ctx) -> {
      RegisterDTO dto = ctx.bodyAsClass(RegisterDTO.class);
      ctx.json(Commons.createResponse(ctx, service.requestRegistration(dto)));
    });

    app.post(Consts.Paths.Auth.COMPLETE_REGISTRATION, (ctx) -> {
      Response res = service.completeRegistration(ctx, ctx.queryParam("token"));
      if (res.isOK()) {
        res = authService.createSession(ctx, res.getData());
      }
      ctx.json(Commons.createResponse(ctx, res));
    });

    app.get(Consts.Paths.Company.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.getCurrentCompany()));
    }, AccessRoles.ANYONE());

    app.get(Consts.Paths.Company.GEO_INFO, (ctx) -> {
      Map<String, String> map = ClientSide.getGeoInfo(ctx.req);
      ctx.json(Commons.createResponse(ctx, new Response(map)));
    }, AccessRoles.ANYONE());

    // create
    app.post(Consts.Paths.Company.BASE, (ctx) -> {
      CreateDTO dto = ctx.bodyAsClass(CreateDTO.class);
      ctx.json(Commons.createResponse(ctx, service.create(dto)));
    }, AccessRoles.ANYONE());

    // update
    app.put(Consts.Paths.Company.BASE, (ctx) -> {
      CreateDTO dto = ctx.bodyAsClass(CreateDTO.class);
      ctx.json(Commons.createResponse(ctx, service.update(dto)));
    }, AccessRoles.ADMIN_ONLY());

    app.put(Consts.Paths.Company.DELETE, (ctx) -> {
      StringDTO dto = ctx.bodyAsClass(StringDTO.class);
      ctx.json(Commons.createResponse(ctx, service.deleteCompany(dto.getValue())));
    }, AccessRoles.ADMIN_ONLY());

  }

}
