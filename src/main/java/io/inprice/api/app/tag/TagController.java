package io.inprice.api.app.tag;

import io.inprice.api.consts.Consts;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class TagController implements Controller {

  private static final TagService service = Beans.getSingleton(TagService.class);

  @Override
  public void addRoutes(Javalin app) {

    // find all tags
    app.get(Consts.Paths.Tag.PRODUCT, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, service.findAll()));
    }, AccessRoles.ANYONE());

  }

}
