package io.inprice.api.app.superuser.platform;

import io.inprice.api.app.superuser.platform.dto.SearchDTO;
import io.inprice.api.app.superuser.platform.dto.PlatformDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.JsonConverter;
import io.javalin.Javalin;

@Router
public class PlatformController extends AbstractController {

  private static final PlatformService service = Beans.getSingleton(PlatformService.class);

  @Override
  public void addRoutes(Javalin app) {

    // search
    app.post(Consts.Paths.Super.Platform.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
    		Response res = service.search(dto);
    		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

    // update
    app.put(Consts.Paths.Super.Platform._BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		PlatformDTO dto = ctx.bodyAsClass(PlatformDTO.class);
    		Response res = service.update(dto);
    		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());
    
    // toggle parked value
    app.put(Consts.Paths.Super.Platform.TOGGLE_PARKED + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.toggleParked(id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // toggle blocked value
    app.put(Consts.Paths.Super.Platform.TOGGLE_BLOCKED + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.toggleBlocked(id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

  }

}