package io.inprice.api.app.superuser.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.superuser.dto.ALSearchBy;
import io.inprice.api.app.superuser.dto.ALSearchDTO;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AccessLogMapper;
import io.inprice.common.meta.UserMarkType;
import io.inprice.common.models.AccessLog;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
import io.inprice.common.models.UserMark;
import io.inprice.common.utils.DateUtils;

class Service {

  private static final Logger log = LoggerFactory.getLogger("SU:User");

  private final RedisClient redis = Beans.getSingleton(RedisClient.class);
  
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

  Response ban(IdTextDTO dto) {
  	String problem = null;
  	
  	if (dto.getId() == CurrentUser.getUserId()) {
  		problem = "You cannot ban yourself!";
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
  						//he must be added in to user_mark table to prevent later registration or forgot pass. requests!
  						superDao.addUserMark(user.getEmail(), UserMarkType.BANNED, dto.getText());

  						//and his accounts too
  						superDao.banAllBoundAccountsOfUser(dto.getId());

  						//his sessions are terminated as well!
              UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
  	          List<String> hashList = userSessionDao.findHashesByUserId(dto.getId());

  	          if (hashList.size() > 0) {
  	          	userSessionDao.deleteByHashList(hashList);
  	          	for (String hash : hashList) redis.removeSesion(hash);
  	          }

  	          handle.commit();
  						return Responses.OK;
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
      			
      			//revoking user's ban
      			Dao superDao = handle.attach(Dao.class);
      			boolean isOK = superDao.revokeBan(id);
      			
      			if (isOK) {
  						//his ban record in user_mark table must be revoked too!
  						superDao.removeUserMark(user.getEmail(), UserMarkType.BANNED);

      				//and from his accounts too
        			int affected = superDao.revokeBanAllBoundAccountsOfUser(id);

        			if (affected > 0) {
    	          handle.commit();
    						return Responses.OK;
    					}
      			}

						handle.rollback();
						return Responses.DataProblem.DB_PROBLEM;
      		} else {
      			return Responses.Already.NOT_BANNED_USER;
      		}
      	}
      }
  	}
  	return Responses.Invalid.USER;
  }

  Response fetchDetails(Long userId) {
  	try (Handle handle = Database.getHandle()) {
  		Dao userDao = handle.attach(Dao.class);
  		User user = userDao.findById(userId);
  		
  		if (user != null) {
  			Dao superDao = handle.attach(Dao.class);
  			List<Membership> membershipList = superDao.fetchMembershipListById(userId);
  			List<ForDatabase> sessionList = superDao.fetchSessionListById(userId);
  			List<UserMark> usedServiceList = superDao.fetchUsedServiceListByEmail(user.getEmail());
  			
  			Map<String, Object> data = new HashMap<>(4);
  			data.put("user", user);
  			data.put("membershipList", membershipList);
  			data.put("sessionList", sessionList);
  			data.put("usedServiceList", usedServiceList);

  			return new Response(data);
  		}
  	}
  	return Responses.NotFound.USER;
  }

  Response fetchMembershipList(Long userId) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		List<Membership> list = superDao.fetchMembershipListById(userId);
  		return new Response(list);
  	}
  }

  Response fetchSessionList(Long userId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<ForDatabase> list = superDao.fetchSessionListById(userId);
			return new Response(list);
  	}
  }
  
  Response fetchUsedServiceList(Long userId) {
  	try (Handle handle = Database.getHandle()) {
  		Dao userDao = handle.attach(Dao.class);
  		User user = userDao.findById(userId);
  		
  		if (user != null) {
  			Dao superDao = handle.attach(Dao.class);
  			List<UserMark> list = superDao.fetchUsedServiceListByEmail(user.getEmail());
  			return new Response(list);
  		}
  	}
  	return Responses.NotFound.USER;
  }

  Response fetchAccountList(Long userId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<Pair<Long, String>> list = superDao.fetchAccountListByUserId(userId);
			return new Response(list);
  	}
  }

  Response deleteUsedService(Long id) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		UserMark used = superDao.findUsedServiceById(id);

  		if (used != null) {
    		boolean isOK = superDao.deleteUsedService(id);
    		if (isOK) {
    			List<UserMark> newList = superDao.fetchUsedServiceListByEmail(used.getEmail());
    			return new Response(newList);
    		} else {
    			return Responses.DataProblem.DB_PROBLEM;
    		}
  		}
  	}
  	return Responses.NotFound.USED_SERVICE;
  }

	Response toggleUnlimitedUsedService(Long id) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		UserMark used = superDao.findUsedServiceById(id);

  		if (used != null) {
    		boolean isOK = superDao.toggleUnlimitedUsedService(id);
    		if (isOK) {
    			List<UserMark> newList = superDao.fetchUsedServiceListByEmail(used.getEmail());
    			return new Response(newList);
    		} else {
    			return Responses.DataProblem.DB_PROBLEM;
    		}
  		}
  	}
  	return Responses.NotFound.USED_SERVICE;
	}

  Response terminateSession(String hash) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		Long userId = superDao.findUserEmailBySessionHash(hash);

  		if (userId != null) {
    		boolean isOK = superDao.deleteSession(hash);
    		if (isOK) {
    			redis.removeSesion(hash);
    			List<ForDatabase> newList = superDao.fetchSessionListById(userId);
    			return new Response(newList);
    		} else {
    			return Responses.DataProblem.DB_PROBLEM;
    		}
  		}
  	}
  	return Responses.NotFound.USER;
  }

  private String buildQueryForAccessLogSearch(ALSearchDTO dto) {
  	if (dto.getUserId() == null) return null;

  	dto = DTOHelper.normalizeSearch(dto, false);

    StringBuilder where = new StringBuilder("select * from access_log ");

    where.append("where user_id = ");
    where.append(dto.getUserId());

    if (dto.getAccountId() != null) {
    	where.append(" and account_id = ");
    	where.append(dto.getAccountId());
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
    	if (ALSearchBy.STATUS.equals(dto.getSearchBy())) {
      	where.append(" and status in (");
        where.append(dto.getTerm());
      	where.append(")");
    	} else {
    		where.append(" and ");
    		where.append(dto.getSearchBy().getFieldName());
      	where.append(" like '%");
        where.append(dto.getTerm());
        where.append("%' ");
    	}
    }

  	where.append(" order by ");
    where.append(dto.getOrderBy().getFieldName());
    where.append(dto.getOrderDir().getDir());

    where.append(" limit ");
    where.append(dto.getRowCount());
    where.append(", ");
    where.append(dto.getRowLimit());
    
    return where.toString();
  }
  
}
