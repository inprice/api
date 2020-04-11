package io.inprice.scrapper.api.app.user;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.session.CurrentUser;
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

      app.put(Consts.Paths.User.PASSWORD, (ctx) -> {
         PasswordDTO dto = ctx.bodyAsClass(PasswordDTO.class);
         dto.setId(CurrentUser.getUserId());
         ctx.json(Commons.createResponse(ctx, service.updatePassword(dto)));
      });

   }

}
