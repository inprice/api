package io.inprice.api.app.group;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.dto.LinkBulkInsertDTO;
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
  	}, AccessRoles.ANYONE());
  	
    app.get(Consts.Paths.Group.ID_NAME_PAIRS + "/:id", (ctx) -> {
  		Long excludedId = ctx.pathParam("id", Long.class).getValue();
      ctx.json(Commons.createResponse(ctx, service.getIdNameList(excludedId)));
    }, AccessRoles.ANYONE());

    // find links and more by id
    app.get(Consts.Paths.Group.LINKS + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findLinksById(id)));
    }, AccessRoles.ANYONE());

    // search
    app.post(Consts.Paths.Group.SEARCH, (ctx) -> {
    	try {
    		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
    		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.ANYONE());
    
    // insert
    app.post(Consts.Paths.Group.BASE, (ctx) -> {
    	try {
    		GroupDTO dto = ctx.bodyAsClass(GroupDTO.class);
      	ctx.json(Commons.createResponse(ctx, service.insert(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Group.BASE, (ctx) -> {
    	try {
    		GroupDTO dto = ctx.bodyAsClass(GroupDTO.class);
        ctx.json(Commons.createResponse(ctx, service.update(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Group.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.delete(id)));
    }, AccessRoles.EDITOR());

    // import links
    app.post(Consts.Paths.Group.IMPORT_LINKS, (ctx) -> {
    	try {
    		LinkBulkInsertDTO dto = ctx.bodyAsClass(LinkBulkInsertDTO.class);
        ctx.json(Commons.createResponse(ctx, service.bulkInsert(dto)));
    	} catch (Exception e) {
    		ctx.status(400);
    		logForInvalidData(ctx, e);
    	}
    }, AccessRoles.EDITOR());

  }

}
