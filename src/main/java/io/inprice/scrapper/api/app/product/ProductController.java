package io.inprice.scrapper.api.app.product;

import java.util.Map;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.api.rest.component.Commons;
import io.javalin.Javalin;

public class ProductController {

   private static final ProductService service = Beans.getSingleton(ProductService.class);

   @Routing
   public void addRoutes(Javalin app) {

      // insert
      app.post(Consts.Paths.Product.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(ProductDTO.class))));
      });

      // update
      app.put(Consts.Paths.Product.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(ProductDTO.class))));
      });

      // delete
      app.delete(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      });

      // find
      app.get(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.findById(id)));
      });

      // toggle active status
      app.put(Consts.Paths.Product.TOGGLE_STATUS + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.toggleStatus(id)));
      });

      // search
      app.get(Consts.Paths.Product.SEARCH, (ctx) -> {
         Map<String, String> searchMap = ControllerHelper.editSearchMap(ctx.queryParamMap());
         ctx.json(Commons.createResponse(ctx, service.search(searchMap)));
      });

      // list
      app.get(Consts.Paths.Product.BASE + "s", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList()));
      });

   }

}
