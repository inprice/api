package io.inprice.api.app.announce;

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

    // mark read as all for a user
    app.put(Consts.Paths.Announce.BASE, (ctx) -> {
    	ctx.json(service.addLogsForCurrentUser());
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // mark read as an announce of a user
    app.put(Consts.Paths.Announce.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.addLogForCurrentUser(id));
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // fetch new announces
    app.get(Consts.Paths.Announce.NEW_ANNOUNCES, (ctx) -> {
    	ctx.json(service.fetchNewAnnounces());
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // search
    app.post(Consts.Paths.Announce.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	    	ctx.json(service.search(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

  }

}
