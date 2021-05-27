package io.inprice.api.app.superuser.account;

import io.inprice.api.app.superuser.account.dto.CreateCouponDTO;
import io.inprice.api.app.superuser.dto.ALSearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class Controller extends AbstractController {

  private static final Service service = Beans.getSingleton(Service.class);

  @Override
  public void addRoutes(Javalin app) {

    // search
    app.post(Consts.Paths.Super.Account.SEARCH, (ctx) -> {
  		BaseSearchDTO dto = ctx.bodyAsClass(BaseSearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.search(dto)));
    }, AccessRoles.SUPER_ONLY());

    // search for access logs
    app.post(Consts.Paths.Super.Account.AL_SEARCH, (ctx) -> {
  		ALSearchDTO dto = ctx.bodyAsClass(ALSearchDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.searchForAccessLog(dto)));
    }, AccessRoles.SUPER_ONLY());
    
    /*-------------------------------------------------------------------------------------*/

    // ban
    app.post(Consts.Paths.Super.Account.BAN, (ctx) -> {
  		IdTextDTO dto = ctx.bodyAsClass(IdTextDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.ban(dto)));
    }, AccessRoles.SUPER_ONLY());

    // revoke ban
    app.put(Consts.Paths.Super.Account.REVOKE_BAN + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.revokeBan(id)));
    }, AccessRoles.SUPER_ONLY());

    /*-------------------------------------------------------------------------------------*/
    
    // fetch account details
    app.get(Consts.Paths.Super.Account.DETAILS + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.fetchDetails(accountId)));
    }, AccessRoles.SUPER_ONLY());

    // fetch account member list
    app.get(Consts.Paths.Super.Account.MEMBER_LIST + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.fetchMemberList(accountId)));
    }, AccessRoles.SUPER_ONLY());

    // fetch account history
    app.get(Consts.Paths.Super.Account.HISTORY + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.fetchHistory(accountId)));
    }, AccessRoles.SUPER_ONLY());
    
    // fetch transaction list
    app.get(Consts.Paths.Super.Account.TRANSACTION_LIST + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.fetchTransactionList(accountId)));
    }, AccessRoles.SUPER_ONLY());

    // fetch account's users list
    app.get(Consts.Paths.Super.Account.ACCOUNT_USERS + "/:accountId", (ctx) -> {
    	Long accountId = ctx.pathParam("accountId", Long.class).check(it -> it > 0).getValue();
    	ctx.json(Commons.createResponse(ctx, service.fetchUserList(accountId)));
    }, AccessRoles.SUPER_ONLY());

    /*-------------------------------------------------------------------------------------*/

    // bind
    app.put(Consts.Paths.Super.Account.BIND + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, service.bind(ctx, id)));
    }, AccessRoles.SUPER_ONLY());
    
    // unbind
    app.post(Consts.Paths.Super.Account.UNBIND, (ctx) -> {
    	ctx.json(Commons.createResponse(ctx, service.unbind(ctx)));
    }, AccessRoles.SUPER_ONLY());

    // create coupon
    app.post(Consts.Paths.Super.Account.COUPON, (ctx) -> {
  		CreateCouponDTO dto = ctx.bodyAsClass(CreateCouponDTO.class);
  		ctx.json(Commons.createResponse(ctx, service.createCoupon(dto)));
    }, AccessRoles.SUPER_ONLY());

  }

}
