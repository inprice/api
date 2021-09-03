package io.inprice.api.app.superuser.ticket;

import io.inprice.api.app.superuser.ticket.dto.ChangeStatusDTO;
import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.JsonConverter;
import io.javalin.Javalin;

@Router
public class TicketController extends AbstractController {

  private static final TicketService service = Beans.getSingleton(TicketService.class);

  @Override
  public void addRoutes(Javalin app) {

  	// find
    app.get(Consts.Paths.Super.Ticket._BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.findById(id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // search
    app.post(Consts.Paths.Super.Ticket.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	  		Response res = service.search(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());
    
    // change status
    app.put(Consts.Paths.Super.Ticket.CHANGE_STATUS, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	ChangeStatusDTO dto = ctx.bodyAsClass(ChangeStatusDTO.class);
	  		Response res = service.changeStatus(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

    // toggle seen (by super user) value
    app.put(Consts.Paths.Super.Ticket.TOGGLE_SEEN_VALUE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.toggleSeenValue(id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
  }

}
