package io.inprice.scrapper.api.app.user;

import java.util.Map;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.UserInfo;
import io.javalin.Javalin;

public class UserController implements Controller {

   private static final UserService service = Beans.getSingleton(UserService.class);

   @Routing
   public void addRoutes(Javalin app) {

      // --------------------------------------------------------
      // ADMIN OPS
      // --------------------------------------------------------
      // insert
      app.post(Consts.Paths.AdminUser.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(UserDTO.class))));
      });

      // update
      app.put(Consts.Paths.AdminUser.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(UserDTO.class))));
      });

      // update password
      app.put(Consts.Paths.AdminUser.PASSWORD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(UserDTO.class))));
      });

      // delete
      app.delete(Consts.Paths.AdminUser.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      });

      // find
      app.get(Consts.Paths.AdminUser.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.findById(id)));
      });

      // toggle active status
      app.put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.toggleStatus(id)));
      });

      // search
      app.get(Consts.Paths.AdminUser.SEARCH, (ctx) -> {
         Map<String, String> searchMap = ControllerHelper.editSearchMap(ctx.queryParamMap());
         ctx.json(Commons.createResponse(ctx, service.search(searchMap)));
      });

      // list
      app.get(Consts.Paths.AdminUser.BASE + "s", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList()));
      });

      // --------------------------------------------------------
      // USER OPS
      // --------------------------------------------------------
      // update. a user can edit only his/her data
      app.put(Consts.Paths.User.BASE, (ctx) -> {
         UserDTO userDTO = ctx.bodyAsClass(UserDTO.class);
         userDTO.setId(UserInfo.getUserId());
         ctx.json(Commons.createResponse(ctx, service.update(userDTO)));
      });

      // update password. a user can edit only his/her password
      app.put(Consts.Paths.User.PASSWORD, (ctx) -> {
         PasswordDTO passwordDTO = ctx.bodyAsClass(PasswordDTO.class);
         passwordDTO.setId(UserInfo.getUserId());
         ctx.json(Commons.createResponse(ctx, service.updatePassword(passwordDTO)));
      });

      // set active workspace
      app.put(Consts.Paths.User.SET_WORKSPACE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.setActiveWorkspace(id, ctx.ip(), ctx.userAgent())));
      });

   }

}
