package io.inprice.api.app.superuser.account;

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
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AccessLogMapper;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.AccessLog;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountHistory;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Member;
import io.inprice.common.models.User;
import io.inprice.common.utils.DateUtils;
import io.javalin.http.Context;

class Service {

  private static final Logger log = LoggerFactory.getLogger("SU:Account");

  private static final CouponService couponService = Beans.getSingleton(CouponService.class);
  
  Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		return new Response(superDao.search(DTOHelper.normalizeSearch(dto, true, false)));
  	}
  }

	public Response searchForAccessLog(ALSearchDTO dto) {
    try (Handle handle = Database.getHandle()) {
    	String searchQuery = buildQueryForAccessLogSearch(dto);
    	if (searchQuery == null) return Responses.BAD_REQUEST;
    	
    	System.out.println(searchQuery);

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

	Response fetchDetails(Long id) {
  	try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
    	Account account = superDao.findById(id);

    	if (account != null) {
    		List<Member> memberList = superDao.fetchMemberList(id);
    		List<AccountHistory> historyList = superDao.fetchHistory(id);
    		List<AccountTrans> transList = superDao.fetchTransactionList(id);

    		Map<String, Object> data = new HashMap<>(4);
    		data.put("account", account);
    		data.put("memberList", memberList);
    		data.put("historyList", historyList);
    		data.put("transList", transList);

    		return new Response(data);
    	}
    }
  	return Responses.NotFound.ACCOUNT;
	}

  Response fetchMemberList(Long accountId) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		List<Member> list = superDao.fetchMemberList(accountId);
  		return new Response(list);
  	}
  }

  Response fetchHistory(Long accountId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<AccountHistory> list = superDao.fetchHistory(accountId);
			return new Response(list);
  	}
  }

  Response fetchTransactionList(Long accountId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<AccountTrans> list = superDao.fetchTransactionList(accountId);
			return new Response(list);
  	}
  }

  Response fetchUserList(Long accountId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<Pair<Long, String>> list = superDao.fetchUserListByAccountId(accountId);
			return new Response(list);
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
      user.setAccid(accountId);
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

  	dto = DTOHelper.normalizeSearch(dto, false);

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
  		crit.append(" and ");
  		crit.append(dto.getSearchBy().getFieldName());
    	crit.append(" like '%");
      crit.append(dto.getTerm());
      crit.append("%' ");
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
