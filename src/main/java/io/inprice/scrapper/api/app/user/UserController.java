package io.inprice.scrapper.api.app.user;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.consts.Consts;
import io.javalin.Javalin;

@Router
public class UserController implements Controller {

   private static final UserService service = Beans.getSingleton(UserService.class);

   @Override
   public void addRoutes(Javalin app) {

      /* update name
      app.put(Consts.Paths.User.BASE + "/:name", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.updateName(ctx.pathParam("name"))));
      });
      */

      // update password
      app.put(Consts.Paths.User.PASSWORD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.updatePassword(ctx.bodyAsClass(PasswordDTO.class))));
      });

      // delete
      app.put(Consts.Paths.User.CHANGE_COMPANY + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeActiveCompany(id)));
      });

   }

}
