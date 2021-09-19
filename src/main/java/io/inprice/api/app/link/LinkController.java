package io.inprice.api.app.link;

import io.inprice.api.app.link.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LinkDeleteDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class LinkController extends AbstractController {

  private static final LinkService service = Beans.getSingleton(LinkService.class);

  @Override
  public void addRoutes(Javalin app) {

    // search
    app.post(Consts.Paths.Link.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	  		ctx.json(service.search(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // get details
    app.get(Consts.Paths.Link.DETAILS + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.getDetails(id));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // delete
    app.delete(Consts.Paths.Link.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	LinkDeleteDTO ldDto = ctx.bodyAsClass(LinkDeleteDTO.class);
	      ctx.json(service.delete(ldDto));
    	}
    }, AccessRoles.EDITOR());

    // move links to another product
    app.post(Consts.Paths.Link.MOVE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	LinkMoveDTO lmDto = ctx.bodyAsClass(LinkMoveDTO.class);
	      ctx.json(service.moveTo(lmDto));
    	}
    }, AccessRoles.EDITOR());

  }

}
