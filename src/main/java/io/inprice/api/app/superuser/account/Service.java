package io.inprice.api.app.superuser.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.coupon.CouponService;
import io.inprice.api.app.superuser.account.dto.CreateCouponDTO;
import io.inprice.api.app.superuser.dto.ALSearchDTO;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.helpers.CookieHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForResponse;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.analytics.AccessLogMapper;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountHistory;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.User;
import io.inprice.common.models.analytics.AccessLog;
import io.inprice.common.utils.DateUtils;
import io.javalin.http.Context;

class Service {

  private static final Logger log = LoggerFactory.getLogger("SU:Account");

  private static final CouponService couponService = Beans.getSingleton(CouponService.class);
  
  Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		return new Response(superDao.search(DTOHelper.normalizeSearch(dto)));
  	}
  }

	Response fetchDetails(Long id) {
  	try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
    	Account account = superDao.findById(id);

    	if (account != null) {
    		List<AccountTrans> transList = superDao.fetchTransactions(id);
    		List<AccountHistory> historyList = superDao.fetchHistory(id);

    		Map<String, Object> data = new HashMap<>(3);
    		data.put("account", account);
    		data.put("transList", transList);
    		data.put("historyList", historyList);

    		return new Response(data);
    	}
    }
  	return Responses.NotFound.ACCOUNT;
	}

	public Response searchForAccessLog(ALSearchDTO dto) {
    try (Handle handle = Database.getHandle()) {
    	String searchQuery = buildQueryForAccessLogSearch(dto);
    	if (searchQuery == null) return Responses.BAD_REQUEST;

    	List<AccessLog> 
      	searchResult = 
      		handle.createQuery(searchQuery)
      			.map(new AccessLogMapper())
    			.list();
      return new Response(searchResult);
    } catch (Exception e) {
      log.error("Failed in search for access logs.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
	}

  Response bind(Context ctx, Long id) {
    try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
      Account account = superDao.findById(id);
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

  Response unbind(Context ctx) {
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

  Response createCoupon(CreateCouponDTO dto) {
		String problem = null;
		
		if (dto.getAccountId() == null) {
			problem = "Invalid account!";
		}
		if (problem == null && dto.getPlanId() == null) {
			problem = "Invalid plan!";
		}
		if (problem == null 
				&& (dto.getDays() == null || dto.getDays() < 14 || dto.getDays() > 365)) {
			problem = "Days info is invalid, it must be between 14-365!";
		}
		if (problem == null 
				&& (StringUtils.isNotBlank(dto.getDescription()) 
						&& (dto.getDescription().length() < 3 || dto.getDescription().length() > 128))) {
			problem = "If given, description must be between 3-128 chars!";
		}
		
		if (problem == null) {
  		try (Handle handle = Database.getHandle()) {
				return 
					couponService.createCoupon(
						handle, 
						dto.getAccountId(), 
						SubsEvent.SUPERUSER_OFFER, 
						dto.getPlanId(), 
						dto.getDays(), 
						dto.getDescription()
					);
  		}
		}
		return new Response(problem);
  }

  private String buildQueryForAccessLogSearch(ALSearchDTO dto) {
  	if (dto.getAccountId() == null) return null;

  	dto = DTOHelper.normalizeSearch(dto);

    StringBuilder crit = new StringBuilder("select * from access_log ");

    crit.append("where account_id = ");
    crit.append(dto.getAccountId());

    if (dto.getUserId() != null) {
    	crit.append(" and user_id = ");
    	crit.append(dto.getUserId());
    }

    if (dto.getMethod() != null) {
    	crit.append(" and method = '");
    	crit.append(dto.getMethod());
    	crit.append("' ");
    }
    
    if (dto.getStartDate() != null) {
    	crit.append(" and created_at >= ");
    	crit.append(DateUtils.formatDateForDB(dto.getStartDate()));
    }

    if (dto.getEndDate() != null) {
    	crit.append(" and created_at <= ");
    	crit.append(DateUtils.formatDateForDB(dto.getEndDate()));
    }
    
    if (StringUtils.isNotBlank(dto.getTerm())) {
    	crit.append(" and path like '%");
      crit.append(dto.getTerm());
      crit.append("%'");
    }

  	crit.append(" order by ");
    crit.append(dto.getOrderBy().getFieldName());
    crit.append(dto.getOrderDir().getDir());
    
    crit.append(" limit ");
    crit.append(dto.getRowCount());
    crit.append(", ");
    crit.append(dto.getRowLimit());
    
    return crit.toString();
  }
  
}
