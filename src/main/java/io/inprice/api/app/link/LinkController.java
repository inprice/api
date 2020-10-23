package io.inprice.api.app.link;

import io.inprice.api.app.link.dto.LinkDTO;
import io.inprice.api.app.link.dto.LinkSearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.LinkStatus;
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

    // delete
    app.delete(Consts.Paths.Link.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
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

    // search
    app.post(Consts.Paths.Link.SEARCH, (ctx) -> {
      LinkSearchDTO searchDto = ctx.bodyAsClass(LinkSearchDTO.class);
      ctx.json(Commons.createResponse(ctx, service.fullSearch(searchDto)));
    }, AccessRoles.ANYONE());

  }

}
