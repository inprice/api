package io.inprice.scrapper.api.app.user;

import java.util.List;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.LongDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.StringDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.helpers.SessionHelper;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.session.info.ForCookie;
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

      app.put(Consts.Paths.User.UPDATE_NAME, (ctx) -> {
         StringDTO dto = ctx.bodyAsClass(StringDTO.class);
         ctx.json(Commons.createResponse(ctx, service.updateName(dto)));
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
