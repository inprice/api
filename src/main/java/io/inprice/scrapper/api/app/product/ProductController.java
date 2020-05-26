package io.inprice.scrapper.api.app.product;

import java.util.Map;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.ProductDTO;
import io.javalin.Javalin;

@Router
public class ProductController implements Controller {

   private static final ProductService service = Beans.getSingleton(ProductService.class);

   @Override
   public void addRoutes(Javalin app) {

      // insert
      app.post(Consts.Paths.Product.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(ProductDTO.class))));
      }, AccessRoles.EDITOR());

      // update
      app.put(Consts.Paths.Product.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(ProductDTO.class))));
      }, AccessRoles.EDITOR());

      // delete
      app.delete(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      }, AccessRoles.EDITOR());

      // find by id
      app.get(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.findById(id)));
      }, AccessRoles.ANYONE());

      // toggle active status
      app.put(Consts.Paths.Product.TOGGLE_STATUS + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.toggleStatus(id)));
      }, AccessRoles.EDITOR());

      // search
      app.get(Consts.Paths.Product.SEARCH, (ctx) -> {
         Map<String, String> searchMap = ControllerHelper.editSearchMap(ctx.queryParamMap());
         ctx.json(Commons.createResponse(ctx, service.search(searchMap)));
      }, AccessRoles.ANYONE());

   }

}
