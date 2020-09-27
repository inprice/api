package io.inprice.api.app.link;

import io.inprice.api.app.link.dto.LinkDTO;
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
    app.post(Consts.Paths.Competitor.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(LinkDTO.class))));
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Competitor.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
    }, AccessRoles.EDITOR());

    // change status to TOBE_RENEWED
    app.put(Consts.Paths.Competitor.RENEW + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.changeStatus(id, LinkStatus.TOBE_RENEWED)));
    }, AccessRoles.EDITOR());

    // change status to PAUSED
    app.put(Consts.Paths.Competitor.PAUSE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.changeStatus(id, LinkStatus.PAUSED)));
    }, AccessRoles.EDITOR());

    // change status to RESUMED
    app.put(Consts.Paths.Competitor.RESUME + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.changeStatus(id, LinkStatus.RESUMED)));
    }, AccessRoles.EDITOR());

  }

}
