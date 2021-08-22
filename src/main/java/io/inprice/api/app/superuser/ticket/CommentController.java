package io.inprice.api.app.superuser.ticket;

import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketCommentDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class CommentController extends AbstractController {

  private static final CommentService service = Beans.getSingleton(CommentService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Super.Ticket.COMMENT, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		TicketCommentDTO dto = ctx.bodyAsClass(TicketCommentDTO.class);
	    	ctx.json(service.insert(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

    // update
    app.put(Consts.Paths.Super.Ticket.COMMENT, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		TicketCommentDTO dto = ctx.bodyAsClass(TicketCommentDTO.class);
	  		ctx.json(service.update(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

  	// delete
    app.delete(Consts.Paths.Super.Ticket.COMMENT + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.delete(id));
    }, AccessRoles.SUPER_ONLY());

  }

}
