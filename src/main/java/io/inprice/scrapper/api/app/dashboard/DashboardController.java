package io.inprice.scrapper.api.app.dashboard;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.consts.Consts;
import io.javalin.Javalin;

@Router
public class DashboardController implements Controller {

   private static final DashboardService service = Beans.getSingleton(DashboardService.class);

   @Override
   public void addRoutes(Javalin app) {

      app.get(Consts.Paths.Misc.DASHBOARD, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.getReport()));
      });

   }

}
