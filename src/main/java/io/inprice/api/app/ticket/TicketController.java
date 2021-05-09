package io.inprice.api.app.ticket;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.dto.TicketCSatDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class TicketController implements Controller {

  private static final TicketService service = Beans.getSingleton(TicketService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Ticket.BASE, (ctx) -> {
    	try {
    		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
      	ctx.json(Commons.createResponse(ctx, service.insert(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    }, AccessRoles.ANYONE());
    
    // update
    app.put(Consts.Paths.Ticket.BASE, (ctx) -> {
    	try {
    		IdTextDTO dto = ctx.bodyAsClass(IdTextDTO.class);
    		ctx.json(Commons.createResponse(ctx, service.update(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    }, AccessRoles.ANYONE());

  	// finds a ticket by id
    app.get(Consts.Paths.Ticket.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findById(id)));
    }, AccessRoles.ANYONE());

  	// deletes a ticket by id
    app.delete(Consts.Paths.Ticket.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(id)));
    }, AccessRoles.ANYONE());

  	// toggles mark-as-read value of a ticket by id
    app.put(Consts.Paths.Ticket.TOGGLE_MARK_AS_READ + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.markAsRead(id)));
    }, AccessRoles.ANYONE());

  	// marks all tickets as read
    app.put(Consts.Paths.Ticket.MARK_ALL_AS_READ, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.markAllAsRead()));
    }, AccessRoles.ANYONE());

    // search
    app.get(Consts.Paths.Ticket.SEARCH, (ctx) -> {
    	String term = ctx.queryParam("term", String.class).getValue();
    	ctx.json(Commons.createResponse(ctx, service.search(term)));
    }, AccessRoles.ANYONE());

  	// finds unread tickets
    app.get(Consts.Paths.Ticket.LIST, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.findUnreadList()));
    }, AccessRoles.ANYONE());

    // sets customer satisfaction level
    app.put(Consts.Paths.Ticket.SET_CSAT_LEVEL, (ctx) -> {
    	try {
    		TicketCSatDTO dto = ctx.bodyAsClass(TicketCSatDTO.class);
      	ctx.json(Commons.createResponse(ctx, service.setSatisfaction(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    	}
    }, AccessRoles.ANYONE());

  }

}
