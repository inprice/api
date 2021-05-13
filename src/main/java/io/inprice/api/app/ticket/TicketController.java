package io.inprice.api.app.ticket;

import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.TicketCSatDTO;
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
    	try {
    		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
      	ctx.json(Commons.createResponse(ctx, service.insert(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.ANYONE());
    
    // update
    app.put(Consts.Paths.Ticket.BASE, (ctx) -> {
    	try {
    		TicketDTO dto = ctx.bodyAsClass(TicketDTO.class);
    		ctx.json(Commons.createResponse(ctx, service.update(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
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

    // search
    app.post(Consts.Paths.Ticket.SEARCH, (ctx) -> {
    	try {
    		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
    		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.ANYONE());

    // sets customer satisfaction level
    app.post(Consts.Paths.Ticket.SET_CSAT, (ctx) -> {
    	try {
    		TicketCSatDTO dto = ctx.bodyAsClass(TicketCSatDTO.class);
      	ctx.json(Commons.createResponse(ctx, service.setSatisfaction(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.ANYONE());

  }

}