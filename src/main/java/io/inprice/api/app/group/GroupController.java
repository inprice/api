package io.inprice.api.app.group;

import io.inprice.api.app.group.dto.AddLinksDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
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
  		ctx.json(service.findById(id));
  	}, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    app.get(Consts.Paths.Group.ID_NAME_PAIRS + "/:id", (ctx) -> {
  		//Long excludedId = ctx.pathParam("id", Long.class).getOrNull();
  		Long excludedId = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		//Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.getIdNameList(excludedId));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // find links and details by id
    app.get(Consts.Paths.Group.LINKS + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.findLinksById(id));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // search
    app.post(Consts.Paths.Group.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
	  		ctx.json(service.search(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_ACCOUNT());

    // insert
    app.post(Consts.Paths.Group.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		GroupDTO dto = ctx.bodyAsClass(GroupDTO.class);
	    	ctx.json(service.insert(dto));
    	}
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Group.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		GroupDTO dto = ctx.bodyAsClass(GroupDTO.class);
	      ctx.json(service.update(dto));
    	}
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Group.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.delete(id));
    }, AccessRoles.EDITOR());

    // add links
    app.post(Consts.Paths.Group.ADD_LINKS, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		AddLinksDTO dto = ctx.bodyAsClass(AddLinksDTO.class);
	      ctx.json(service.addLinks(dto));
    	}
    }, AccessRoles.EDITOR());

  }

}
