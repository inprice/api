package io.inprice.scrapper.api.app.competitor;

import java.util.Map;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.dto.CompetitorDTO;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.javalin.Javalin;

@Router
public class CompetitorController implements Controller {

   private static final CompetitorService service = Beans.getSingleton(CompetitorService.class);

   @Override
   public void addRoutes(Javalin app) {

      // insert
      app.post(Consts.Paths.Competitor.BASE, (ctx) -> {
         ctx.json(Commons.createResponse(ctx, service.insert(ctx.bodyAsClass(CompetitorDTO.class))));
      }, AccessRoles.EDITOR());

      // delete
      app.delete(Consts.Paths.Competitor.BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.deleteById(id)));
      }, AccessRoles.EDITOR());

      // list
      app.get(Consts.Paths.Competitor.BASE + "s/:prod_id", (ctx) -> {
         Long prodId = ctx.pathParam("prod_id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.getList(prodId)));
      }, AccessRoles.ANYONE());

      // search
      app.get(Consts.Paths.Competitor.SEARCH, (ctx) -> {
        Map<String, String> searchMap = ControllerHelper.editSearchMap(ctx.queryParamMap(), "status");
        ctx.json(Commons.createResponse(ctx, service.search(searchMap)));
      }, AccessRoles.ANYONE());

      // change status to TOBE_RENEWED
      app.put(Consts.Paths.Competitor.RENEW + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, CompetitorStatus.TOBE_RENEWED)));
      }, AccessRoles.EDITOR());

      // change status to PAUSED
      app.put(Consts.Paths.Competitor.PAUSE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, CompetitorStatus.PAUSED)));
      }, AccessRoles.EDITOR());

      // change status to RESUMED
      app.put(Consts.Paths.Competitor.RESUME + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, service.changeStatus(id, CompetitorStatus.RESUMED)));
      }, AccessRoles.EDITOR());

   }

}