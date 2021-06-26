package io.inprice.api.app.superuser.ticket;

import io.inprice.api.app.superuser.ticket.dto.ChangeStatusDTO;
import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
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

  	// find
    app.get(Consts.Paths.Super.Ticket._BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findById(id)));
    }, AccessRoles.SUPER_ONLY());

    // search
    app.post(Consts.Paths.Super.Ticket.SEARCH, (ctx) -> {
  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.SUPER_ONLY());
    
    // change status
    app.put(Consts.Paths.Super.Ticket.CHANGE_STATUS, (ctx) -> {
    	ChangeStatusDTO dto = ctx.bodyAsClass(ChangeStatusDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.changeStatus(dto)));
    }, AccessRoles.SUPER_ONLY());

    // toggle seen (by user) value
    app.put(Consts.Paths.Super.Ticket.TOGGLE_SEEN_VALUE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		ctx.json(Commons.createResponse(ctx, service.toggleSeenValue(id)));
    }, AccessRoles.SUPER_ONLY());
    
  }

}