package io.inprice.api.app.user;

import java.util.List;

import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.LongDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForCookie;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class UserController implements Controller {

   private static final UserService service = Beans.getSingleton(UserService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.put(Consts.Paths.User.PASSWORD, (ctx) -> {
         PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
         dto.setId(CurrentUser.getUserId());
         ctx.json(Commons.createResponse(ctx, service.updatePassword(dto)));
      }, AccessRoles.ANYONE());

      app.put(Consts.Paths.User.UPDATE, (ctx) -> {
         UserDTO dto = ctx.bodyAsClass(UserDTO.class);
         ctx.json(Commons.createResponse(ctx, service.update(dto)));
      }, AccessRoles.ANYONE());

      app.get(Consts.Paths.User.INVITATIONS, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getInvitations()));
      }, AccessRoles.ANYONE());

      app.put(Consts.Paths.User.ACCEPT_INVITATION, (ctx) -> {
         LongDTO dto = ctx.bodyAsClass(LongDTO.class);
         ctx.json(Commons.createResponse(ctx, service.acceptInvitation(dto)));
      }, AccessRoles.ANYONE());

      app.put(Consts.Paths.User.REJECT_INVITATION, (ctx) -> {
         LongDTO dto = ctx.bodyAsClass(LongDTO.class);
         ctx.json(Commons.createResponse(ctx, service.rejectInvitation(dto)));
      }, AccessRoles.ANYONE());

      app.get(Consts.Paths.User.MEMBERSHIPS, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getMemberships()));
      }, AccessRoles.ANYONE());

      app.put(Consts.Paths.User.LEAVE_MEMBERSHIP, (ctx) -> {
         LongDTO dto = ctx.bodyAsClass(LongDTO.class);
         ctx.json(Commons.createResponse(ctx, service.leaveMember(dto)));
      }, AccessRoles.ANYONE());

      app.get(Consts.Paths.User.OPENED_SESSIONS, (ctx) -> {
         String tokenString = ctx.cookie(Consts.SESSION);
         List<ForCookie> cookieSesList = SessionHelper.fromToken(tokenString);
         ctx.json(Commons.createResponse(ctx, service.getOpenedSessions(cookieSesList)));
      }, AccessRoles.ANYONE());

      app.post(Consts.Paths.User.CLOSE_ALL_SESSIONS, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.closeAllSessions()));
      }, AccessRoles.ANYONE());

   }

}
