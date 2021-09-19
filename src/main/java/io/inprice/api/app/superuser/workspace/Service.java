package io.inprice.api.app.superuser.workspace;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.app.coupon.CouponDao;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.superuser.workspace.dto.CreateCouponDTO;
import io.inprice.api.app.superuser.dto.ALSearchDTO;
import io.inprice.api.app.system.SystemDao;
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
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AccessLogMapper;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.AccessLog;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.WorkspaceHistory;
import io.inprice.common.models.WorkspaceTrans;
import io.inprice.common.models.Membership;
import io.inprice.common.models.Plan;
import io.inprice.common.models.User;
import io.inprice.common.utils.CouponManager;
import io.inprice.common.utils.DateUtils;
import io.javalin.http.Context;

class Service {

  private static final Logger logger = LoggerFactory.getLogger("SU:Workspace");
  
  Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
      dto = DTOHelper.normalizeSearch(dto, true, false);
      dto.setTerm(dto.getTerm());
      return new Response(superDao.search(dto));
  	}
  }

	Response searchForAccessLog(ALSearchDTO dto) {
  	if (dto.getWorkspaceId() != null && dto.getWorkspaceId() > 0) {
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
        logger.error("Failed in search for access logs.", e);
        return Responses.ServerProblem.EXCEPTION;
      }
  	}
  	return new Response("Workspace id is missing!");
	}

	Response fetchDetails(Long id) {
  	try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
    	Workspace workspace = superDao.findById(id);

    	if (workspace != null) {
    		List<Membership> memberList = superDao.fetchMemberList(id);
    		List<WorkspaceHistory> historyList = superDao.fetchHistory(id);
    		List<WorkspaceTrans> transList = superDao.fetchTransactionList(id);

    		Map<String, Object> data = Map.of(
    			"workspace", workspace,
    			"memberList", memberList,
    			"historyList", historyList,
    			"transList", transList
  			);
    		return new Response(data);
    	}
    }
  	return Responses.NotFound.WORKSPACE;
	}

  Response fetchMemberList(Long workspaceId) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		List<Membership> list = superDao.fetchMemberList(workspaceId);
			return new Response(list);
  	}
  }

  Response fetchHistory(Long workspaceId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<WorkspaceHistory> list = superDao.fetchHistory(workspaceId);
			return new Response(list);
  	}
  }

  Response fetchTransactionList(Long workspaceId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<WorkspaceTrans> list = superDao.fetchTransactionList(workspaceId);
			return new Response(list);
  	}
  }

  Response fetchUserList(Long workspaceId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<Pair<Long, String>> list = superDao.fetchUserListByWorkspaceId(workspaceId);
			return new Response(list);
  	}
  }

  Response bind(Context ctx, Long id) {
    try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
      Workspace workspace = superDao.findById(id);
      if (workspace != null) {
      	ForResponse session = 
    			new ForResponse(
  					workspace.getId(),
  					CurrentUser.getUserName(),
  					CurrentUser.getEmail(),
  					CurrentUser.getUserTimezone()
					);
      	session.setWorkspace(workspace.getName());
      	session.setWorkspaceStatus(workspace.getStatus().name());
      	session.setLinkCount(workspace.getLinkCount());
      	session.setSubsStartedAt(workspace.getSubsStartedAt());
      	session.setSubsRenewalAt(workspace.getSubsRenewalAt());
        session.setLastStatusUpdate(workspace.getLastStatusUpdate());
      	session.setCurrencyFormat(workspace.getCurrencyCode());
      	session.setPlanId(workspace.getPlanId());
      	if (workspace.getPlan() != null) session.setPlanName(workspace.getPlan().getName());
      	
      	boolean isOK = refreshSuperCookie(handle, ctx, id);
      	if (isOK) {
      		return new Response(session);
      	} else {
      		return Responses.BAD_REQUEST;
      	}
      }
    }
    return Responses.NotFound.WORKSPACE;
  }

  Response unbind(Context ctx) {
  	if (CurrentUser.getWorkspaceId() != null) {
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
  	}
    return new Response("You haven't bound to an workspace!");
  }

  private boolean refreshSuperCookie(Handle handle, Context ctx, Long workspaceId) {
    UserDao userDao = handle.attach(UserDao.class);
    User user = userDao.findById(CurrentUser.getUserId());
    if (user != null) {
      user.setAccid(workspaceId);
    	ctx.cookie(CookieHelper.createSuperCookie(SessionHelper.toTokenForSuper(user)));
    	return true;
    }
    return false;
  }

  Response createCoupon(CreateCouponDTO dto) {
		String problem = null;
		
		if (dto.getWorkspaceId() == null || dto.getWorkspaceId() < 1) {
			problem = "Workspace id is missing!";
		}
		if (problem == null && dto.getPlanId() == null) {
			problem = "Invalid plan!";
		}
		if (problem == null 
				&& (dto.getDays() == null || dto.getDays() < 14 || dto.getDays() > 365)) {
			problem = "Days info is invalid, it must be between 14 - 365!";
		}
		if (problem == null 
				&& (StringUtils.isNotBlank(dto.getDescription()) && dto.getDescription().length() > 128)) {
			problem = "Description can be up to 128 chars!";
		}
		
		if (problem == null) {
  		try (Handle handle = Database.getHandle()) {
				return 
					createCoupon(
						handle, 
						dto.getWorkspaceId(), 
						SubsEvent.GIVEN_COUPON, 
						dto.getPlanId(), 
						dto.getDays(), 
						dto.getDescription()
					);
  		}
		}
		return new Response(problem);
  }

  private Response createCoupon(Handle handle, long workspaceId, SubsEvent subsEvent, Integer planId, long days, String description) {
  	Response res = Responses.NotFound.WORKSPACE;

  	SystemDao planDao = handle.attach(SystemDao.class);
  	Plan plan = planDao.findById(planId);
  	if (plan != null) {

    	WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
    	Workspace workspace = workspaceDao.findById(workspaceId);
  		if (workspace != null) {
  			
  			if (workspace.getStatus().isOKForCoupon()) {
  				
  				boolean hasLimitProblem = false;
  				if (workspace.getPlanId() != null && workspace.getPlanId() != plan.getId()) {
  					hasLimitProblem = (
  							workspace.getUserCount() > plan.getUserLimit()+1 
    	  				|| workspace.getLinkCount() > plan.getLinkLimit() 
    	  				|| workspace.getAlarmCount() > plan.getAlarmLimit()
    	  			);
  				}

  	  		if (hasLimitProblem == false) {
  	  	    String couponCode = CouponManager.generate();
  	  	    CouponDao couponDao = handle.attach(CouponDao.class);
  	  	    boolean isOK = couponDao.create(
  	  	      couponCode,
  	  	      planId,
  	  	      days,
  	  	      description,
  	  	      workspaceId
  	  	    );
  
  	  	    if (isOK) {
  	  	      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
  	  	      WorkspaceTrans trans = new WorkspaceTrans();
  	  	      trans.setWorkspaceId(workspaceId);
  	  	      trans.setEventId(couponCode);
  	  	      trans.setEvent(subsEvent);
  	  	      trans.setSuccessful(Boolean.TRUE);
  	  	      trans.setReason(description);
  	  	      trans.setDescription("Issued coupon");
  	  	      subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
  	  	      res = new Response(Map.of("code", couponCode));
  	  	    } else {
  	  	    	res = Responses.DataProblem.DB_PROBLEM;
  	  	    }
  	  		} else {
  	  			res = new Response("Current limits of this workspace are greater than the plan's!");
  	  		}
				} else {
					res = Responses.Already.ACTIVE_SUBSCRIPTION;
				}
  		}
		} else {
			res = Responses.NotFound.PLAN;
		}

  	return res;
  }

  Response searchIdNameList(String term) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		return new Response(superDao.searchIdNameListByName("%" + SqlHelper.clear(term) + "%"));
  	}
  }

  private String buildQueryForAccessLogSearch(ALSearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, false);

    StringBuilder where = new StringBuilder("select * from access_log ");

    where.append("where workspace_id = ");
    where.append(dto.getWorkspaceId());

    if (dto.getUserId() != null) {
    	where.append(" and user_id = ");
    	where.append(dto.getUserId());
    }

    if (dto.getMethod() != null) {
    	where.append(" and method = '");
    	where.append(dto.getMethod());
    	where.append("' ");
    }
    
    if (dto.getStartDate() != null) {
    	where.append(" and created_at >= ");
    	where.append(DateUtils.formatDateForDB(dto.getStartDate()));
    }

    if (dto.getEndDate() != null) {
    	where.append(" and created_at <= ");
    	where.append(DateUtils.formatDateForDB(dto.getEndDate()));
    }
    
    if (StringUtils.isNotBlank(dto.getTerm())) {
  		where.append(" and ");
  		where.append(dto.getSearchBy().getFieldName());
    	where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

  	where.append(" order by ");
    where.append(dto.getOrderBy().getFieldName());
    where.append(dto.getOrderDir().getDir());
    where.append(", id ");

    where.append(" limit ");
    where.append(dto.getRowCount());
    where.append(", ");
    where.append(dto.getRowLimit());
    
    return where.toString();
  }
  
}
