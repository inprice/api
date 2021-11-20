package io.inprice.api.app.product;

import io.inprice.api.app.product.dto.AddLinksDTO;
import io.inprice.api.app.product.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class ProductController extends AbstractController {

  private static final ProductService service = Beans.getSingleton(ProductService.class);

  @Override
  public void addRoutes(Javalin app) {

    // find by id
  	app.get(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
  		Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		ctx.json(service.findById(id));
  	}, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    app.get(Consts.Paths.Product.ID_NAME_PAIRS + "/:id", (ctx) -> {
  		Long excludedId = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.getIdNameList(excludedId));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // find links and details by id
    app.get(Consts.Paths.Product.LINKS + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.findLinksById(id));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // search
    app.post(Consts.Paths.Product.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
	  		ctx.json(service.search(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // insert
    app.post(Consts.Paths.Product.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		ProductDTO dto = ctx.bodyAsClass(ProductDTO.class);
	    	ctx.json(service.insert(dto));
    	}
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Product.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		ProductDTO dto = ctx.bodyAsClass(ProductDTO.class);
	      ctx.json(service.update(dto));
    	}
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.delete(id));
    }, AccessRoles.EDITOR());

    // add links
    app.post(Consts.Paths.Product.ADD_LINKS, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		AddLinksDTO dto = ctx.bodyAsClass(AddLinksDTO.class);
	      ctx.json(service.addLinks(dto));
    	}
    }, AccessRoles.EDITOR());

  }

}
