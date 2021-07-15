package io.inprice.api.app.announce;

import io.inprice.api.app.announce.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class AnnounceController extends AbstractController {

  private static final AnnounceService service = Beans.getSingleton(AnnounceService.class);

  @Override
  public void addRoutes(Javalin app) {

    // add logs for a user
    app.put(Consts.Paths.Announce.BASE, (ctx) -> {
    	ctx.json(Commons.createResponse(ctx, service.addLogsForCurrentUser()));
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // fetch new announces
    app.get(Consts.Paths.Announce.NEW_ANNOUNCES, (ctx) -> {
    	ctx.json(Commons.createResponse(ctx, service.fetchNewAnnounces()));
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // search
    app.post(Consts.Paths.Announce.SEARCH, (ctx) -> {
    	SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

  }

}
