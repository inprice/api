package io.inprice.api.app.category;

import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.dto.StringDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class CategoryController extends AbstractController {

  private static final CategoryService service = Beans.getSingleton(CategoryService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Definitions.CATEGORY, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		StringDTO dto = ctx.bodyAsClass(StringDTO.class);
	  		ctx.json(service.insert(dto.getValue()));
    	}
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Definitions.CATEGORY, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		IdTextDTO dto = ctx.bodyAsClass(IdTextDTO.class);
	  		ctx.json(service.update(dto));
    	}
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Definitions.CATEGORY + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.delete(id));
    }, AccessRoles.EDITOR());

    // list
    app.get(Consts.Paths.Definitions.CATEGORY + "/list", (ctx) -> {
  		ctx.json(service.list());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // search
    app.post(Consts.Paths.Definitions.CATEGORY + "s/search", (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		StringDTO dto = ctx.bodyAsClass(StringDTO.class);
	  		ctx.json(service.search(dto.getValue()));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

  }

}
