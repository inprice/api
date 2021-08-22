package io.inprice.api.app.superuser.announce;

import io.inprice.api.app.announce.dto.AnnounceDTO;
import io.inprice.api.app.announce.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
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
	    	ctx.json(service.insert(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

    // update
    app.put(Consts.Paths.Super.Announce._BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		AnnounceDTO dto = ctx.bodyAsClass(AnnounceDTO.class);
	  		ctx.json(service.update(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

  	// delete
    app.delete(Consts.Paths.Super.Announce._BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.delete(id));
    }, AccessRoles.SUPER_ONLY());
    
    // search
    app.post(Consts.Paths.Super.Announce.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	    	ctx.json(service.search(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

  }

}
