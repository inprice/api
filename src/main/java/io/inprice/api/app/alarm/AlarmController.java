package io.inprice.api.app.alarm;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.dto.SetAlarmOFFDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.AlarmTopic;
import io.javalin.Javalin;

@Router
public class AlarmController extends AbstractController {

  private static final AlarmService service = Beans.getSingleton(AlarmService.class);

  @Override
  public void addRoutes(Javalin app) {

    // insert
    app.post(Consts.Paths.Alarm.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	AlarmDTO dto = ctx.bodyAsClass(AlarmDTO.class);
	    	ctx.json(service.insert(dto));
    	}
    }, AccessRoles.EDITOR());
    
    // update
    app.put(Consts.Paths.Alarm.BASE, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	AlarmDTO dto = ctx.bodyAsClass(AlarmDTO.class);
	    	ctx.json(service.update(dto));
    	}
    }, AccessRoles.EDITOR());

    // delete or set off!
    app.delete(Consts.Paths.Alarm.BASE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.delete(id));
    }, AccessRoles.EDITOR());

    // search
    app.post(Consts.Paths.Alarm.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
    		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
	    	ctx.json(service.search(dto));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // get details
    app.get(Consts.Paths.Alarm.DETAILS + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.getDetails(id));
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // returns id name pairs by topic
    app.get(Consts.Paths.Alarm.ID_NAME_PAIRS + "/:topic", (ctx) -> {
    	AlarmTopic topic = null;
    	try {
    		topic = AlarmTopic.valueOf(ctx.pathParam("topic"));
			} catch (Exception e) { }
    	if (topic == null) {
    		ctx.json(Responses.Invalid.ALARM_TOPIC);
    	} else {
    		ctx.json(service.getIdNameList(topic));
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    // set alarm off
    app.put(Consts.Paths.Link.ALARM_OFF, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	    	SetAlarmOFFDTO dto = ctx.bodyAsClass(SetAlarmOFFDTO.class);
	      ctx.json(service.setAlarmOFF(dto));
    	}
    }, AccessRoles.EDITOR());

  }

}
