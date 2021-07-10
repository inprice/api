package io.inprice.api.app.user;

import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.LongDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class UserController extends AbstractController {

	private static final UserService service = Beans.getSingleton(UserService.class);

	@Override
	public void addRoutes(Javalin app) {

		app.get(Consts.Paths.User.INVITATIONS, (ctx) -> {
			ctx.json(Commons.createResponse(ctx, service.getInvitations()));
		}, AccessRoles.ANYONE());

		app.get(Consts.Paths.User.MEMBERSHIPS, (ctx) -> {
			ctx.json(Commons.createResponse(ctx, service.getMemberships()));
		}, AccessRoles.ANYONE());

		app.get(Consts.Paths.User.OPENED_SESSIONS, (ctx) -> {
			ctx.json(Commons.createResponse(ctx, service.getOpenedSessions(ctx)));
		}, AccessRoles.ANYONE());
		
		/*----------------------------------------------------------------------------*/

		app.put(Consts.Paths.User.UPDATE_INFO, (ctx) -> {
			UserDTO dto = ctx.bodyAsClass(UserDTO.class);
			ctx.json(Commons.createResponse(ctx, service.updateInfo(dto)));
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.ACCEPT_INVITATION, (ctx) -> {
			LongDTO dto = ctx.bodyAsClass(LongDTO.class);
			ctx.json(Commons.createResponse(ctx, service.acceptInvitation(dto)));
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.REJECT_INVITATION, (ctx) -> {
			LongDTO dto = ctx.bodyAsClass(LongDTO.class);
			ctx.json(Commons.createResponse(ctx, service.rejectInvitation(dto)));
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.LEAVE_MEMBERSHIP, (ctx) -> {
			LongDTO dto = ctx.bodyAsClass(LongDTO.class);
			ctx.json(Commons.createResponse(ctx, service.leaveMember(dto)));
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.post(Consts.Paths.User.CLOSE_ALL_SESSIONS, (ctx) -> {
			ctx.json(Commons.createResponse(ctx, service.closeAllSessions()));
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

		app.put(Consts.Paths.User.CHANGE_PASSWORD, (ctx) -> {
			PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
			ctx.json(Commons.createResponse(ctx, service.changePassword(dto)));
		}, AccessRoles.ANYONE_EXCEPT_SUPER());

	}

}
