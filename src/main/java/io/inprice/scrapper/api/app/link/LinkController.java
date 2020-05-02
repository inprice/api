package io.inprice.scrapper.api.app.link;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.javalin.Javalin;

@Router
public class LinkController implements Controller {

   private static final LinkService service = Beans.getSingleton(LinkService.class);

   @Override
   public void addRoutes(Javalin app) {

      // insert
      app.post(Consts.Paths.Link.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(LinkDTO.class))));
      }, AccessRoles.EDITOR());

      // update
      app.put(Consts.Paths.Link.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(LinkDTO.class))));
      }, AccessRoles.EDITOR());

      // delete
      app.delete(Consts.Paths.Link.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      }, AccessRoles.EDITOR());

      // list
      app.get(Consts.Paths.Link.BASE + "s/:prod_id", (ctx) -> {
         Long prodId = ctx.pathParam("prod_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.getList(prodId)));
      }, AccessRoles.ANYONE());

      // change status to RENEWED
      app.put(Consts.Paths.Link.RENEW + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, LinkStatus.RENEWED)));
      }, AccessRoles.EDITOR());

      // change status to PAUSED
      app.put(Consts.Paths.Link.PAUSE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, LinkStatus.PAUSED)));
      }, AccessRoles.EDITOR());

      // change status to RESUMED
      app.put(Consts.Paths.Link.RESUME + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, LinkStatus.RESUMED)));
      }, AccessRoles.EDITOR());

   }

}
