package io.inprice.api.app.link;

import io.inprice.api.app.link.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.LinkDeleteDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class LinkController extends AbstractController {

  private static final LinkService service = Beans.getSingleton(LinkService.class);

  @Override
  public void addRoutes(Javalin app) {
    
    // delete
    app.delete(Consts.Paths.Link.BASE, (ctx) -> {
    	LinkDeleteDTO ldDto = ctx.bodyAsClass(LinkDeleteDTO.class);
      ctx.json(Commons.createResponse(ctx, service.delete(ldDto)));
    }, AccessRoles.EDITOR());

    // move links to under another group
    app.post(Consts.Paths.Link.MOVE, (ctx) -> {
    	LinkMoveDTO lmDto = ctx.bodyAsClass(LinkMoveDTO.class);
      ctx.json(Commons.createResponse(ctx, service.moveTo(lmDto)));
    }, AccessRoles.EDITOR());

    // search
    app.post(Consts.Paths.Link.SEARCH, (ctx) -> {
  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.ANYONE());
    
    // get details
    app.get(Consts.Paths.Link.DETAILS + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.getDetails(id)));
    }, AccessRoles.ANYONE());

    // change status to PAUSED | RESUMED
    /*
    app.put(Consts.Paths.Link.TOGGLE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.toggleStatus(id)));
    }, AccessRoles.EDITOR());
    */

  }

}
