package io.inprice.api.app.lookup;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.LookupDTO;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.LookupType;
import io.javalin.Javalin;

@Router
public class LookupController implements Controller {

   private static final LookupService service = Beans.getSingleton(LookupService.class);

   @Override
   public void addRoutes(Javalin app) {

      // insert
      app.post(Consts.Paths.Lookup.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.add(ctx.bodyAsClass(LookupDTO.class))));
      }, AccessRoles.EDITOR());

      // get brand list
      app.get(Consts.Paths.Lookup.BASE + "/" + LookupType.BRAND.name().toLowerCase(), (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList(LookupType.BRAND)));
      }, AccessRoles.EDITOR());

      // get category list
      app.get(Consts.Paths.Lookup.BASE + "/" + LookupType.CATEGORY.name().toLowerCase(), (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getList(LookupType.CATEGORY)));
      }, AccessRoles.EDITOR());

   }

}
