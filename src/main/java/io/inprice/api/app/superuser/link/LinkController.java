package io.inprice.api.app.superuser.link;

import io.inprice.api.app.superuser.link.dto.BulkChangetDTO;
import io.inprice.api.app.superuser.link.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
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
    app.post(Consts.Paths.Super.Link.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	  		ctx.json(service.search(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

    // get details
    app.get(Consts.Paths.Super.Link.DETAILS + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.fetchDetails(id));
    }, AccessRoles.SUPER_ONLY());

    // marks the links as PAUSED, RESOLVED and NOT_SUITABLE
    app.put(Consts.Paths.Super.Link.CHANGE_STATUS, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		BulkChangetDTO dto = ctx.bodyAsClass(BulkChangetDTO.class);
	  		ctx.json(service.changeStatus(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

    // undo the last transaction if only if it is marked as PAUSED, RESOLVED or NOT_SUITABLE
    app.put(Consts.Paths.Super.Link.UNDO, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		BulkChangetDTO dto = ctx.bodyAsClass(BulkChangetDTO.class);
    		ctx.json(service.undo(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

  }

}