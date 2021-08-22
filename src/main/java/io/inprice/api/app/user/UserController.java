package io.inprice.api.app.user;

import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LongDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class UserController extends AbstractController {

	private static final UserService service = Beans.getSingleton(UserService.class);

	@Override
	public void addRoutes(Javalin app) {

		app.get(Consts.Paths.User.INVITATIONS, (ctx) -> {
			ctx.json(service.getInvitations());
		}, AccessRoles.ANYONE());

		app.get(Consts.Paths.User.MEMBERSHIPS, (ctx) -> {
			ctx.json(service.getMemberships());
		}, AccessRoles.ANYONE());

		app.get(Consts.Paths.User.OPENED_SESSIONS, (ctx) -> {
			ctx.json(service.getOpenedSessions(ctx));
		}, AccessRoles.ANYONE());
		
		/*----------------------------------------------------------------------------*/

		app.put(Consts.Paths.User.UPDATE_INFO, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
				UserDTO dto = ctx.bodyAsClass(UserDTO.class);
				ctx.json(service.updateInfo(dto));
    	}
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.ACCEPT_INVITATION, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
				LongDTO dto = ctx.bodyAsClass(LongDTO.class);
				ctx.json(service.acceptInvitation(dto));
    	}
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.REJECT_INVITATION, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
				LongDTO dto = ctx.bodyAsClass(LongDTO.class);
				ctx.json(service.rejectInvitation(dto));
    	}
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.LEAVE_MEMBERSHIP, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
				LongDTO dto = ctx.bodyAsClass(LongDTO.class);
				ctx.json(service.leaveMembership(dto));
    	}
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.post(Consts.Paths.User.CLOSE_ALL_SESSIONS, (ctx) -> {
			ctx.json(service.closeAllSessions());
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.CHANGE_PASSWORD, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
				PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
				ctx.json(service.changePassword(dto));
    	}
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

	}

}
