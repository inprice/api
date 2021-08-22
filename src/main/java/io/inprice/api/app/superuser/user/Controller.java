package io.inprice.api.app.superuser.user;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.app.superuser.dto.ALSearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class Controller extends AbstractController {

  private static final Service service = Beans.getSingleton(Service.class);

  @Override
  public void addRoutes(Javalin app) {

    // search
    app.post(Consts.Paths.Super.User.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
	  		ctx.json(service.search(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

    // search for access logs
    app.post(Consts.Paths.Super.User.AL_SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		ALSearchDTO dto = ctx.bodyAsClass(ALSearchDTO.class);
	  		ctx.json(service.searchForAccessLog(dto));
    	}
    }, AccessRoles.SUPER_ONLY());
    
    /*-------------------------------------------------------------------------------------*/

    // ban
    app.post(Consts.Paths.Super.User.BAN, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		IdTextDTO dto = ctx.bodyAsClass(IdTextDTO.class);
	  		ctx.json(service.ban(dto));
    	}
    }, AccessRoles.SUPER_ONLY());

    // revoke ban
    app.put(Consts.Paths.Super.User.REVOKE_BAN + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(service.revokeBan(id));
    }, AccessRoles.SUPER_ONLY());

    /*-------------------------------------------------------------------------------------*/
    
    // fetch user details
    app.get(Consts.Paths.Super.User.DETAILS + "/:userId", (ctx) -> {
    	Long userId = ctx.pathParam("userId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.fetchDetails(userId));
    }, AccessRoles.SUPER_ONLY());

    // fetch user membership list
    app.get(Consts.Paths.Super.User.MEMBERSHIP_LIST + "/:userId", (ctx) -> {
    	Long userId = ctx.pathParam("userId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.fetchMembershipList(userId));
    }, AccessRoles.SUPER_ONLY());

    // fetch user session list
    app.get(Consts.Paths.Super.User.SESSION_LIST + "/:userId", (ctx) -> {
    	Long userId = ctx.pathParam("userId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.fetchSessionList(userId));
    }, AccessRoles.SUPER_ONLY());
    
    // fetch used services list
    app.get(Consts.Paths.Super.User.USED_SERVICE + "s/:userId", (ctx) -> {
    	Long userId = ctx.pathParam("userId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.fetchUsedServiceList(userId));
    }, AccessRoles.SUPER_ONLY());

    // fetch user's account list
    app.get(Consts.Paths.Super.User.USER_ACCOUNTS + "/:userId", (ctx) -> {
    	Long userId = ctx.pathParam("userId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.fetchAccountList(userId));
    }, AccessRoles.SUPER_ONLY());

    /*-------------------------------------------------------------------------------------*/

    // terminate session
    app.delete(Consts.Paths.Super.User.TERMINATE_SESSION + "/:hash", (ctx) -> {
    	String hash = ctx.pathParam("hash", String.class).check(it -> StringUtils.isNotBlank(it)).getValue();
    	ctx.json(service.terminateSession(hash));
    }, AccessRoles.SUPER_ONLY());
    
    /*-------------------------------------------------------------------------------------*/

    // delete used service
    app.delete(Consts.Paths.Super.User.USED_SERVICE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
    	ctx.json(service.deleteUsedService(id));
    }, AccessRoles.SUPER_ONLY());

    // toggle used service for unlimited use
    app.put(Consts.Paths.Super.User.USED_SERVICE_TOGGLE + "/:id", (ctx) -> {
    	Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		ctx.json(service.toggleUnlimitedUsedService(id));
    }, AccessRoles.SUPER_ONLY());

  }

}
