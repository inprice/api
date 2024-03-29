package io.inprice.api.app.superuser.workspace;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.superuser.workspace.dto.CreateVoucherDTO;
import io.inprice.api.app.system.PlanDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.workspace.WorkspaceDao;
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
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.models.Membership;
import io.inprice.common.models.Plan;
import io.inprice.common.models.User;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.WorkspaceHistory;
import io.inprice.common.models.WorkspaceTrans;
import io.inprice.common.utils.VoucherManager;
import io.javalin.http.Context;

class Service {

  Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
      dto = DTOHelper.normalizeSearch(dto, true, false);
      dto.setTerm(dto.getTerm());
      return new Response(superDao.search(dto));
  	}
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
  					CurrentUser.getFullName(),
  					CurrentUser.getEmail(),
  					CurrentUser.getUserTimezone()
					);
      	session.setWorkspace(workspace.getName());
      	session.setWorkspaceStatus(workspace.getStatus());
      	session.setProductCount(workspace.getProductCount());
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
      				CurrentUser.getFullName(),
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
      user.setWsId(workspaceId);
    	ctx.cookie(CookieHelper.createSuperCookie(SessionHelper.toTokenForSuper(user)));
    	return true;
    }
    return false;
  }

  Response createVoucher(CreateVoucherDTO dto) {
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
					createVoucher(
						handle, 
						dto.getWorkspaceId(), 
						SubsEvent.GIVEN_VOUCHER, 
						dto.getPlanId(), 
						dto.getDays(), 
						dto.getDescription()
					);
  		}
		}
		return new Response(problem);
  }

  private Response createVoucher(Handle handle, long workspaceId, SubsEvent subsEvent, Integer planId, long days, String description) {
  	Response res = Responses.NotFound.WORKSPACE;

  	PlanDao planDao = handle.attach(PlanDao.class);
  	Plan plan = planDao.findById(planId);
  	if (plan != null) {

    	WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
    	Workspace workspace = workspaceDao.findById(workspaceId);
  		if (workspace != null) {
  			
  			if (WorkspaceStatus.BANNED.equals(workspace.getStatus()) == false) {
	  	    String voucherCode = VoucherManager.generate();
	  	    Dao dao = handle.attach(Dao.class);
	  	    boolean isOK = dao.createVoucher(
	  	      voucherCode,
	  	      planId,
	  	      days,
	  	      description,
	  	      workspaceId
	  	    );

	  	    if (isOK) {
	  	      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
	  	      WorkspaceTrans trans = new WorkspaceTrans();
	  	      trans.setWorkspaceId(workspaceId);
	  	      trans.setEventId(voucherCode);
	  	      trans.setEvent(subsEvent);
	  	      trans.setSuccessful(Boolean.TRUE);
	  	      trans.setReason(description);
	  	      trans.setDescription("Issued voucher");
	  	      subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
	  	      res = new Response(Map.of("code", voucherCode));
	  	    } else {
	  	    	res = Responses.DataProblem.DB_PROBLEM;
	  	    }
				} else {
					res = Responses.Already.BANNED_WORKSPACE;
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

}
