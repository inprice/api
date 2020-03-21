package io.inprice.scrapper.api.app.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.rest.component.Commons;
import io.javalin.Javalin;

public class LinkController {

   private static final Logger log = LoggerFactory.getLogger(LinkController.class);
   private static final LinkService service = Beans.getSingleton(LinkService.class);

   @Routing
   public void addRoutes(Javalin app) {

      // insert
      app.post(Consts.Paths.Link.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(LinkDTO.class))));
      });

      // delete
      app.delete(Consts.Paths.Link.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      });

      // find
      app.get(Consts.Paths.Link.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.findById(id)));
      });

      // list
      app.get(Consts.Paths.Link.BASE + "s/:product_id", (ctx) -> {
         Long productId = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.getList(productId)));
      });

      // change status to RENEWED
      app.put(Consts.Paths.Link.RENEW + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         Long productId = ctx.queryParam("product_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, productId, Status.RENEWED)));
      });

      // change status to PAUSED
      app.put(Consts.Paths.Link.RENEW + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         Long productId = ctx.queryParam("product_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, productId, Status.PAUSED)));
      });

      // change status to RESUMED
      app.put(Consts.Paths.Link.RESUME + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         Long productId = ctx.queryParam("product_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, productId, Status.PAUSED)));
      });

   }

}
