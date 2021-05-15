package io.inprice.api.app.superuser;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.helpers.CookieHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForResponse;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Account;
import io.inprice.common.models.User;
import io.javalin.http.Context;

class SuperService {

  Response searchAccount(BaseSearchDTO dto) {
    try (Handle handle = Database.getHandle()) {
    	SuperDao superDao = handle.attach(SuperDao.class);
    	return new Response(superDao.searchAccount(DTOHelper.normalizeSearch(dto)));
    }
  }

  Response bindAccount(Context ctx, Long id) {
    try (Handle handle = Database.getHandle()) {
    	SuperDao superDao = handle.attach(SuperDao.class);
      Account account = superDao.findAccountById(id);
      if (account != null) {
      	ForResponse session = 
    			new ForResponse(
  					account.getId(),
  					CurrentUser.getUserName(),
  					CurrentUser.getEmail(),
  					CurrentUser.getUserTimezone()
					);
      	session.setAccount(account.getName());
      	session.setAccountStatus(account.getStatus().name());
      	session.setLinkCount(account.getLinkCount());
      	session.setSubsStartedAt(account.getSubsStartedAt());
      	session.setSubsRenewalAt(account.getSubsRenewalAt());
        session.setLastStatusUpdate(account.getLastStatusUpdate());
      	session.setCurrencyFormat(account.getCurrencyCode());
      	session.setPlanId(account.getPlanId());
      	if (account.getPlan() != null) session.setPlanName(account.getPlan().getName());
      	
      	boolean isOK = refreshSuperCookie(handle, ctx, id);
      	if (isOK) {
      		return new Response(session);
      	} else {
      		return Responses.BAD_REQUEST;
      	}
      }
    }
    return Responses.NotFound.ACCOUNT;
  }

  Response unbindAccount(Context ctx) {
    try (Handle handle = Database.getHandle()) {
    	boolean isOK = refreshSuperCookie(handle, ctx, null);
    	if (isOK) {
      	return new Response ( 
    			new ForResponse(
    				null,
    				CurrentUser.getUserName(),
    				CurrentUser.getEmail(),
    				CurrentUser.getUserTimezone()
    			)
    		);
    	}
    }
    return Responses.BAD_REQUEST;
  }
  
  private boolean refreshSuperCookie(Handle handle, Context ctx, Long accountId) {
    UserDao userDao = handle.attach(UserDao.class);
    User user = userDao.findById(CurrentUser.getUserId());
    if (user != null) {
      user.setAccountId(accountId);
    	ctx.cookie(CookieHelper.createSuperCookie(SessionHelper.toTokenForSuper(user)));
    	return true;
    }
    return false;
  }

}
