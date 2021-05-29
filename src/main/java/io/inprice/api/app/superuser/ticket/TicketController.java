package io.inprice.api.app.superuser.ticket;

import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class TicketController extends AbstractController {

  private static final TicketService service = Beans.getSingleton(TicketService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Ticket.BASE, (ctx) -> {
  		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.insert(dto)));
    }, AccessRoles.SUPER_ONLY());

    // update
    app.put(Consts.Paths.Ticket.BASE, (ctx) -> {
  		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.update(dto)));
    }, AccessRoles.SUPER_ONLY());

  	// find
    app.get(Consts.Paths.Ticket.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findById(id)));
    }, AccessRoles.SUPER_ONLY());

  	// delete
    app.delete(Consts.Paths.Ticket.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(id)));
    }, AccessRoles.SUPER_ONLY());

    // search
    app.post(Consts.Paths.Ticket.SEARCH, (ctx) -> {
  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.SUPER_ONLY());
    
  }

}
