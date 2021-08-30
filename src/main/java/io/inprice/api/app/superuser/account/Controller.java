package io.inprice.api.app.superuser.account;

import io.inprice.api.app.superuser.account.dto.CreateCouponDTO;
import io.inprice.api.app.superuser.dto.ALSearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.JsonConverter;
import io.javalin.Javalin;

@Router
public class Controller extends AbstractController {

  private static final Service service = Beans.getSingleton(Service.class);

  @Override
  public void addRoutes(Javalin app) {

    // search
    app.post(Consts.Paths.Super.Account.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
	  		Response res = service.search(dto);
    		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

    // search for id name list
    app.post(Consts.Paths.Super.Account.ID_NAME_PAIRS, (ctx) -> {
  		String term = ctx.queryParam("term", String.class).getValue();
  		Response res = service.searchIdNameList(term);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // search for access logs
    app.post(Consts.Paths.Super.Account.AL_SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		ALSearchDTO dto = ctx.bodyAsClass(ALSearchDTO.class);
	  		Response res = service.searchForAccessLog(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());
    
    /*-------------------------------------------------------------------------------------*/
    
    // fetch account details
    app.get(Consts.Paths.Super.Account.DETAILS + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchDetails(accountId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // fetch account member list
    app.get(Consts.Paths.Super.Account.MEMBER_LIST + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchMemberList(accountId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // fetch account history
    app.get(Consts.Paths.Super.Account.HISTORY + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchHistory(accountId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
    // fetch transaction list
    app.get(Consts.Paths.Super.Account.TRANSACTION_LIST + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchTransactionList(accountId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // fetch account's users list
    app.get(Consts.Paths.Super.Account.ACCOUNT_USERS + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchUserList(accountId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    /*-------------------------------------------------------------------------------------*/

    // bind
    app.put(Consts.Paths.Super.Account.BIND + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.bind(ctx, id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
    // unbind
    app.post(Consts.Paths.Super.Account.UNBIND, (ctx) -> {
  		Response res = service.unbind(ctx);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // create coupon
    app.post(Consts.Paths.Super.Account.COUPON, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		CreateCouponDTO dto = ctx.bodyAsClass(CreateCouponDTO.class);
	  		Response res = service.createCoupon(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

  }

}
