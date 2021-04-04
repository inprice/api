package io.inprice.api.app.group;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.dto.LinkBulkInsertDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class GroupController implements Controller {

  private static final GroupService service = Beans.getSingleton(GroupService.class);

  @Override
  public void addRoutes(Javalin app) {

    // find by id
    app.get(Consts.Paths.Group.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findById(id)));
    }, AccessRoles.ANYONE());

    // find links and more by id
    app.get(Consts.Paths.Group.LINKS + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findLinksById(id)));
    }, AccessRoles.ANYONE());

    // search
    app.get(Consts.Paths.Group.SEARCH, (ctx) -> {
    	String term = ctx.queryParam("term", String.class).getValue();
    	ctx.json(Commons.createResponse(ctx, service.search(term)));
    }, AccessRoles.ANYONE());
    
    // insert
    app.post(Consts.Paths.Group.BASE, (ctx) -> {
    	ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(GroupDTO.class))));
    }, AccessRoles.EDITOR());

    // import links
    app.post(Consts.Paths.Group.IMPORT_LINKS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.bulkInsert(ctx.bodyAsClass(LinkBulkInsertDTO.class))));
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Group.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(GroupDTO.class))));
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Group.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(id)));
    }, AccessRoles.EDITOR());

    // move links to under another group
    app.put(Consts.Paths.Group.MOVE_LINKS, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.moveLinks(ctx.bodyAsClass(LinkMoveDTO.class))));
    }, AccessRoles.EDITOR());

  }

}
