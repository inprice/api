package io.inprice.api.app.superuser.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.superuser.user.dto.AccessLogSearchDTO;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.analytics.AccessLogMapper;
import io.inprice.common.models.Member;
import io.inprice.common.models.User;
import io.inprice.common.models.UserUsed;
import io.inprice.common.models.analytics.AccessLog;
import io.inprice.common.utils.DateUtils;

class Service {

  private static final Logger log = LoggerFactory.getLogger("SuperUser:User");

	Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
    	return new Response(superDao.search(DTOHelper.normalizeSearch(dto)));
    }
	}

	Response fetchDetails(Long id) {
  	try (Handle handle = Database.getHandle()) {
    	Dao userDao = handle.attach(Dao.class);
    	User user = userDao.findById(id);

    	if (user != null) {
    		Dao superDao = handle.attach(Dao.class);
    		List<UserUsed> usedList = superDao.fetchUsedListByEmail(user.getEmail());
    		List<ForDatabase> sessionList = superDao.fetchSessionListById(user.getId());
        List<Member> membershipList = superDao.fetchMembershipListById(user.getId());

        AccessLogSearchDTO dto = new AccessLogSearchDTO();
        dto.setUserId(id); //is a mandatory!

        List<AccessLog> 
        	accessLogList = 
        		handle.createQuery(buildQueryForAccessLogSearch(dto))
        			.map(new AccessLogMapper())
      			.list();

    		Map<String, Object> data = new HashMap<>(5);
    		data.put("user", user);
    		data.put("membershipList", membershipList);
    		data.put("usedList", usedList);
    		data.put("sessionList", sessionList);
    		data.put("accessLogList", accessLogList);

    		return new Response(data);
    	}
    }
  	return Responses.NotFound.ACCOUNT;
	}
	
	public Response searchForAccessLog(AccessLogSearchDTO dto) {
    try (Handle handle = Database.getHandle()) {
    	String searchQuery = buildQueryForAccessLogSearch(dto);
    	if (searchQuery == null) return Responses.BAD_REQUEST;

    	List<AccessLog> 
      	searchResult = 
      		handle.createQuery(searchQuery)
      			.map(new AccessLogMapper())
    			.list();
      return new Response(Collections.singletonMap("rows", searchResult));
    } catch (Exception e) {
      log.error("Failed in search for access logs.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
	}

  Response ban(IdTextDTO dto) {
  	String problem = null;
  	
  	if (dto.getId() == CurrentUser.getUserId()) {
  		problem = "Invalid user!";
  	}
  	if (problem == null 
  			&& (StringUtils.isBlank(dto.getText()) 
  					|| dto.getText().length() < 5 || dto.getText().length() > 128)) {
  		problem = "Reason must be between 5-128 chars!";
  	}
  	
  	if (problem == null) {
  		try (Handle handle = Database.getHandle()) {
  			UserDao userDao = handle.attach(UserDao.class);
  			User user = userDao.findById(dto.getId());

  			if (user != null) {
  				if (! user.isBanned()) {
  					
  					handle.begin();
  					
  					//user is banned
  					Dao superDao = handle.attach(Dao.class);
  					boolean isOK = superDao.ban(dto.getId(), dto.getText());

  					if (isOK) {
  						
  						//so do his accounts
  						AccountDao accountDao = handle.attach(AccountDao.class);
  						accountDao.banAllBoundAccountsOfUser(dto.getId());

  						//his sessions are terminated as well!
              UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
  	          List<String> hashList = userSessionDao.findHashesByUserId(dto.getId());

  	          if (hashList.size() > 0) {
  	          	userSessionDao.deleteByHashList(hashList);
  	          	for (String hash : hashList) RedisClient.removeSesion(hash);
  	          }

  	          handle.commit();
  						return Responses.OK;
  					} else {
  						handle.rollback();
  						return Responses.DataProblem.DB_PROBLEM;
  					}
  				} else {
  					return Responses.Already.BANNED_USER;
  				}
  			}
  		}
  	}
  	return new Response(problem);
  }

  Response revokeBan(Long id) {
  	if (id != CurrentUser.getUserId()) {
      try (Handle handle = Database.getHandle()) {
      	UserDao userDao = handle.attach(UserDao.class);
      	User user = userDao.findById(id);

      	if (user != null) {
      		if (user.isBanned()) {

      			handle.begin();
      			
      			//revoking user ban
      			Dao superDao = handle.attach(Dao.class);
      			boolean isOK = superDao.revokeBan(id);

      			if (isOK) {

  						//and from his accounts
  						AccountDao accountDao = handle.attach(AccountDao.class);
  						accountDao.revokeBanAllBoundAccountsOfUser(id);
      				
  	          handle.commit();
  						return Responses.OK;
  					} else {
  						handle.rollback();
  						return Responses.DataProblem.DB_PROBLEM;
      			}
      		} else {
      			return Responses.Already.NOT_BANNED_USER;
      		}
      	}
      }
  	}
  	return Responses.Invalid.USER;
  }

  private String buildQueryForAccessLogSearch(AccessLogSearchDTO dto) {
  	if (dto.getUserId() == null) return null;

  	dto = DTOHelper.normalizeSearch(dto);

    StringBuilder crit = new StringBuilder("select * from analytics_access_log ");

    crit.append("where user_id = ");
    crit.append(dto.getUserId());

    if (dto.getAccountId() != null) {
    	crit.append(" and account_id = ");
    	crit.append(dto.getAccountId());
    }

    if (StringUtils.isNotBlank(dto.getMethod())) {
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

    crit.append(" limit ");
    crit.append(dto.getRowCount());
    crit.append(", ");
    crit.append(dto.getRowLimit());
    
    return crit.toString();
  }
  
}
