package io.inprice.api.app.alarm;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class AlarmController extends AbstractController {

  private static final AlarmService service = Beans.getSingleton(AlarmService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Alarm.BASE, (ctx) -> {
    	AlarmDTO dto = ctx.bodyAsClass(AlarmDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.insert(dto)));
    }, AccessRoles.EDITOR());
    
    // update
    app.put(Consts.Paths.Alarm.BASE, (ctx) -> {
    	AlarmDTO dto = ctx.bodyAsClass(AlarmDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.update(dto)));
    }, AccessRoles.EDITOR());

    // delete or set off!
    app.delete(Consts.Paths.Alarm.BASE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.delete(id)));
    }, AccessRoles.EDITOR());

    // search
    app.post(Consts.Paths.Alarm.SEARCH, (ctx) -> {
    	SearchDTO dto = ctx.bodyAsClass(SearchDTO.class);
    	ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.EDITOR());

  }

}
