package io.inprice.api.app.superuser.announce;

import io.inprice.api.app.announce.dto.AnnounceDTO;
import io.inprice.api.app.announce.dto.SearchDTO;
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
public class AnnounceController extends AbstractController {

  private static final AnnounceService service = Beans.getSingleton(AnnounceService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Super.Announce._BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		AnnounceDTO dto = ctx.bodyAsClass(AnnounceDTO.class);
	  		Response res = service.insert(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

    // update
    app.put(Consts.Paths.Super.Announce._BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		AnnounceDTO dto = ctx.bodyAsClass(AnnounceDTO.class);
	  		Response res = service.update(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

  	// delete
    app.delete(Consts.Paths.Super.Announce._BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.delete(id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
    // search
    app.post(Consts.Paths.Super.Announce.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	  		Response res = service.search(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

  }

}
