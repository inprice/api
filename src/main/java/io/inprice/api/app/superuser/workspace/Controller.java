package io.inprice.api.app.superuser.workspace;

import io.inprice.api.app.superuser.workspace.dto.CreateVoucherDTO;
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
    app.post(Consts.Paths.Super.Workspace.SEARCH, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
	  		Response res = service.search(dto);
    		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

    // search for id name list
    app.post(Consts.Paths.Super.Workspace.ID_NAME_PAIRS, (ctx) -> {
  		String term = ctx.queryParam("term", String.class).getValue();
  		Response res = service.searchIdNameList(term);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
    /*-------------------------------------------------------------------------------------*/
    
    // fetch workspace details
    app.get(Consts.Paths.Super.Workspace.DETAILS + "/:workspaceId", (ctx) -> {
    	Long workspaceId = ctx.pathParam("workspaceId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchDetails(workspaceId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // fetch workspace member list
    app.get(Consts.Paths.Super.Workspace.MEMBER_LIST + "/:workspaceId", (ctx) -> {
    	Long workspaceId = ctx.pathParam("workspaceId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchMemberList(workspaceId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // fetch workspace history
    app.get(Consts.Paths.Super.Workspace.HISTORY + "/:workspaceId", (ctx) -> {
    	Long workspaceId = ctx.pathParam("workspaceId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchHistory(workspaceId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
    // fetch transaction list
    app.get(Consts.Paths.Super.Workspace.TRANSACTION_LIST + "/:workspaceId", (ctx) -> {
    	Long workspaceId = ctx.pathParam("workspaceId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchTransactionList(workspaceId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // fetch workspace's users list
    app.get(Consts.Paths.Super.Workspace.WORKSPACE_USERS + "/:workspaceId", (ctx) -> {
    	Long workspaceId = ctx.pathParam("workspaceId", Long.class).check(it -> it > 0).getValue();
  		Response res = service.fetchUserList(workspaceId);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    /*-------------------------------------------------------------------------------------*/

    // bind
    app.put(Consts.Paths.Super.Workspace.BIND + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
  		Response res = service.bind(ctx, id);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());
    
    // unbind
    app.post(Consts.Paths.Super.Workspace.UNBIND, (ctx) -> {
  		Response res = service.unbind(ctx);
  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    }, AccessRoles.SUPER_ONLY());

    // create voucher
    app.post(Consts.Paths.Super.Workspace.VOUCHER, (ctx) -> {
    	if (ctx.body().isBlank()) {
    		ctx.json(Responses.REQUEST_BODY_INVALID);
    	} else {
	  		CreateVoucherDTO dto = ctx.bodyAsClass(CreateVoucherDTO.class);
	  		Response res = service.createVoucher(dto);
	  		ctx.result(JsonConverter.toJsonWithoutIgnoring(res));
    	}
    }, AccessRoles.SUPER_ONLY());

  }

}
