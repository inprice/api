package io.inprice.api.app.product;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.ProductSearchDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.info.ProductDTO;
import io.javalin.Javalin;

@Router
public class ProductController implements Controller {

  private static final ProductService service = Beans.getSingleton(ProductService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Product.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(ProductDTO.class))));
    }, AccessRoles.EDITOR());

    // update
    app.put(Consts.Paths.Product.BASE, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.update(ctx.bodyAsClass(ProductDTO.class))));
    }, AccessRoles.EDITOR());

    // delete
    app.delete(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
    }, AccessRoles.EDITOR());

    // find by id
    app.get(Consts.Paths.Product.BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findById(id)));
    }, AccessRoles.ANYONE());

    // find everything by id
    app.get(Consts.Paths.Product.EVERYTHING + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.findEverythingById(id)));
    }, AccessRoles.ANYONE());

    // toggle active status
    app.put(Consts.Paths.Product.TOGGLE_STATUS + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.toggleStatus(id)));
    }, AccessRoles.EDITOR());

    // search
    app.post(Consts.Paths.Product.SEARCH, (ctx) -> {
      Response res = null;
      String searchTerm = ctx.queryParam("term");
      if (StringUtils.isNotBlank(searchTerm)) {
        res = service.simpleSearch(searchTerm);
      } else {
        res = service.fullSearch(ctx.bodyAsClass(ProductSearchDTO.class));
      }
      ctx.json(Commons.createResponse(ctx, res));
    }, AccessRoles.ANYONE());

  }

}
