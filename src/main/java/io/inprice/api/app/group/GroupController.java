package io.inprice.api.app.group;

import io.inprice.api.app.group.dto.AddLinksDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class GroupController extends AbstractController {

  private static final GroupService service = Beans.getSingleton(GroupService.class);

  @Override
  public void addRoutes(Javalin app) {

    // find by id
  	app.get(Consts.Paths.Group.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		ctx.json(Commons.createResponse(ctx, service.findById(id)));
  	}, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    app.get(Consts.Paths.Group.ID_NAME_PAIRS + "/:id", (ctx) -> {
  		//Long excludedId = ctx.pathParam("id", Long.class).getOrNull();
  		Long excludedId = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		//Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.getIdNameList(excludedId)));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // find links and details by id
    app.get(Consts.Paths.Group.LINKS + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findLinksById(id)));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // search
    app.post(Consts.Paths.Group.SEARCH, (ctx) -> {
  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // insert
    app.post(Consts.Paths.Group.BASE, (ctx) -> {
  		GroupDTO dto = ctx.bodyAsClass(GroupDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.insert(dto)));
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Group.BASE, (ctx) -> {
  		GroupDTO dto = ctx.bodyAsClass(GroupDTO.class);
      ctx.json(Commons.createResponse(ctx, service.update(dto)));
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Group.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(id)));
    }, AccessRoles.EDITOR());

    // add links
    app.post(Consts.Paths.Group.ADD_LINKS, (ctx) -> {
  		AddLinksDTO dto = ctx.bodyAsClass(AddLinksDTO.class);
      ctx.json(Commons.createResponse(ctx, service.addLinks(dto)));
    }, AccessRoles.EDITOR());

  }

}
