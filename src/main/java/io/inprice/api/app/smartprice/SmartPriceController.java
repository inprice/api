package io.inprice.api.app.smartprice;

import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.StringDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.formula.SmartPriceDTO;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class SmartPriceController extends AbstractController {

  private static final SmartPriceService service = Beans.getSingleton(SmartPriceService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.SmartPrice.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		SmartPriceDTO dto = ctx.bodyAsClass(SmartPriceDTO.class);
	  		ctx.json(service.insert(dto));
    	}
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.SmartPrice.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		SmartPriceDTO dto = ctx.bodyAsClass(SmartPriceDTO.class);
	  		ctx.json(service.update(dto));
    	}
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.SmartPrice.BASE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.delete(id));
    }, AccessRoles.EDITOR());

    // get
    app.get(Consts.Paths.SmartPrice.BASE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.findById(id));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // test
    app.post(Consts.Paths.SmartPrice.TEST, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		SmartPriceDTO dto = ctx.bodyAsClass(SmartPriceDTO.class);
	  		ctx.json(service.test(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // list
    app.get(Consts.Paths.SmartPrice.BASE + "/list", (ctx) -> {
  		ctx.json(service.list());
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // search
    app.post(Consts.Paths.SmartPrice.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		StringDTO dto = ctx.bodyAsClass(StringDTO.class);
	  		ctx.json(service.search(dto.getValue()));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

  }

}
