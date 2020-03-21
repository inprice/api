package io.inprice.scrapper.api.app.dashboard;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.rest.component.Commons;
import io.javalin.Javalin;

public class DashboardController {

   private static final DashboardService service = Beans.getSingleton(DashboardService.class);

   @Routing
   public void addRoutes(Javalin app) {

      app.get(Consts.Paths.Misc.DASHBOARD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getReport()));
      });

   }

}
