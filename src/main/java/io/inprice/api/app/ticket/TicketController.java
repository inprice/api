package io.inprice.api.app.ticket;

import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class TicketController extends AbstractController {

  private static final TicketService service = Beans.getSingleton(TicketService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Ticket.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
	    	ctx.json(service.insert(dto));
    	}
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

    // update
    app.put(Consts.Paths.Ticket.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
	  		ctx.json(service.update(dto));
    	}
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

  	// delete
    app.delete(Consts.Paths.Ticket.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.delete(id));
    }, AccessRoles.ANYONE_EXCEPT_SUPER());

  	// find
    app.get(Consts.Paths.Ticket.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.findById(id));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // search
    app.post(Consts.Paths.Ticket.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	  		ctx.json(service.search(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // toggle seen (by user) value
    app.put(Consts.Paths.Ticket.TOGGLE_SEEN_VALUE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		ctx.json(service.toggleSeenValue(id));
    }, AccessRoles.ANYONE_EXCEPT_SUPER());
    
  }

}
