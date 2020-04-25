package io.inprice.scrapper.api.app.user;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.StringDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.session.CurrentUser;
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

      app.get(Consts.Paths.User.OPENED_SESSIONS, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getOpenedSessions()));
      }, AccessRoles.ANYONE());

      app.post(Consts.Paths.User.CLOSE_ALL_SESSIONS, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.closeAllSessions()));
      }, AccessRoles.ANYONE());

   }

}
