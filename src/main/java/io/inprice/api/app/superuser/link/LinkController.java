package io.inprice.api.app.superuser.link;

import io.inprice.api.app.superuser.link.dto.BulkChangetDTO;
import io.inprice.api.app.superuser.link.dto.SearchDTO;
import io.inprice.api.consts.Consts;
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

    // search
    app.post(Consts.Paths.Super.Link.SEARCH, (ctx) -> {
  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.SUPER_ONLY());

    // get details
    app.get(Consts.Paths.Super.Link.DETAILS + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.getDetails(id)));
    }, AccessRoles.SUPER_ONLY());

    // for PAUSED, RESOLVED and NOT_SUITABLE changings
    app.put(Consts.Paths.Super.Link.CHANGE_STATUS, (ctx) -> {
  		BulkChangetDTO dto = ctx.bodyAsClass(BulkChangetDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.changeStatus(dto)));
    }, AccessRoles.SUPER_ONLY());

    // undo the last transaction if it is PAUSED, RESOLVED or NOT_SUITABLE
    app.put(Consts.Paths.Super.Link.UNDO, (ctx) -> {
  		BulkChangetDTO dto = ctx.bodyAsClass(BulkChangetDTO.class);
      ctx.json(Commons.createResponse(ctx, service.undo(dto)));
    }, AccessRoles.SUPER_ONLY());

  }

}