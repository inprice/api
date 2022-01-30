package io.inprice.api.app.superuser.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.auth.UserSessionDao;
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
import io.inprice.common.meta.Marks;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
import io.inprice.common.models.UserMarks;

class Service {

  private final RedisClient redis = Beans.getSingleton(RedisClient.class);
  
	Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
      dto = DTOHelper.normalizeSearch(dto, true, false);
      dto.setTerm("%"+dto.getTerm());
      return new Response(superDao.search(dto));
    }
	}

  Response ban(IdTextDTO dto) {
  	String problem = validate(dto);

  	if (problem == null) {
  		try (Handle handle = Database.getHandle()) {
  			UserDao userDao = handle.attach(UserDao.class);
  			User user = userDao.findById(dto.getId());

  			if (user != null) {
  				if (user.isBanned() == false) {
  					//if (Consts.EXCLUSIVE_EMAILS.contains(user.getEmail()) == false) {

	  					Dao superDao = handle.attach(Dao.class);
	  					handle.begin();
	  					
	  					//user is banned
	  					boolean isOK = superDao.ban(dto.getId(), dto.getText());
	
	  					if (isOK) {
	  						//he must be added in to user_marks table to prevent later registration or forgot pass. requests!
	  						superDao.addUserMark(user.getEmail(), Marks.BANNED.name(), dto.getText());
	
	  						//and his workspaces too
	  						superDao.banAllBoundWorkspacesOfUser(dto.getId());
	
	  						//his sessions are terminated as well!
	              UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
	  	          List<String> hashList = userSessionDao.findHashesByUserId(dto.getId());
	
	  	          if (hashList.size() > 0) {
	  	          	userSessionDao.deleteByHashList(hashList);
	  	          	redis.removeSesions(hashList);
	  	          }
	
	  	          handle.commit();
	  	          Map<String, String> dataMap = Map.of(
	  	          	"reason", dto.getText()
	          		);
	  						return new Response(dataMap);
	  					}
    				//} else {
    				//	return Responses.NotSuitable.EMAIL;
    				//}
  				} else {
  					return Responses.Already.BANNED_USER;
  				}
  			} else {
  				return Responses.NotFound.USER;
  			}
  		}
  	}

  	return new Response(problem);
  }

  Response revokeBan(Long id) {
  	Response res = Responses.NotFound.USER;

  	if (id != CurrentUser.getUserId()) {
      try (Handle handle = Database.getHandle()) {
      	UserDao userDao = handle.attach(UserDao.class);
      	User user = userDao.findById(id);

      	if (user != null) {
      		if (user.isBanned()) {

      			Dao superDao = handle.attach(Dao.class);
      			handle.begin();
      			
      			//revoking user's ban
      			boolean isOK = superDao.revokeBan(id);
      			
      			if (isOK) {
  						//his ban record in user_marks table must be revoked as well!
  						superDao.removeUserMark(user.getEmail(), Marks.BANNED.name());

      				//and from his workspaces too
        			superDao.revokeBanAllBoundWorkspacesOfUser(id);

  	          handle.commit();
  	          res = Responses.OK;
      			} else {
  						handle.rollback();
  						res = Responses.DataProblem.DB_PROBLEM;
      			}
      		} else {
      			res = Responses.Already.NOT_BANNED_USER;
      		}
      	}
      }
  	} else {
  		res = new Response("You cannot revoke your ban!");
  	}

  	return res;
  }

  Response fetchDetails(Long userId) {
  	try (Handle handle = Database.getHandle()) {
  		Dao userDao = handle.attach(Dao.class);
  		User user = userDao.findById(userId);
  		
  		if (user != null) {
  			Dao superDao = handle.attach(Dao.class);
  			List<Membership> membershipList = superDao.fetchMembershipListById(userId);
  			List<ForDatabase> sessionList = superDao.fetchSessionListById(userId);
  			List<UserMarks> usedServiceList = superDao.fetchUsedServiceListByEmail(user.getEmail());
  			
  			user.setPassword(null);
  			
  			Map<String, Object> data = Map.of(
  				"user", user,
  				"membershipList", membershipList,
  				"sessionList", sessionList,
  				"usedServiceList", usedServiceList
				);
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
  			List<UserMarks> list = superDao.fetchUsedServiceListByEmail(user.getEmail());
  			return new Response(list);
  		}
  	}
  	return Responses.NotFound.USER;
  }

  Response fetchWorkspaceList(Long userId) {
  	try (Handle handle = Database.getHandle()) {
			Dao superDao = handle.attach(Dao.class);
			List<Pair<Long, String>> list = superDao.fetchWorkspaceListByUserId(userId);
			return new Response(list);
  	}
  }

  Response deleteUsedService(Long id) {
  	try (Handle handle = Database.getHandle()) {
  		Dao superDao = handle.attach(Dao.class);
  		UserMarks used = superDao.findUsedServiceById(id);

  		if (used != null) {
    		boolean isOK = superDao.deleteUsedService(id);
    		if (isOK) {
    			List<UserMarks> newList = superDao.fetchUsedServiceListByEmail(used.getEmail());
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
  		UserMarks used = superDao.findUsedServiceById(id);

  		if (used != null) {
    		boolean isOK = superDao.toggleUnlimitedUsedService(id);
    		if (isOK) {
    			List<UserMarks> newList = superDao.fetchUsedServiceListByEmail(used.getEmail());
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

  Response terminateAllSessions(Long userId) {
  	try (Handle handle = Database.getHandle()) {
      UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);

      List<ForDatabase> sessions = userSessionDao.findListByUserId(userId);
      if (CollectionUtils.isNotEmpty(sessions)) {
      	List<String> hashList = new ArrayList<>(sessions.size());
        for (ForDatabase ses : sessions) hashList.add(ses.getHash());
        redis.removeSesions(hashList);

        if (userSessionDao.deleteByUserId(userId)) {
          return Responses.OK;
        }
      }
  	}
  	return Responses.NotFound.USER;
  }
  
  private String validate(IdTextDTO dto) {
  	String problem = null;

  	if (dto.getId() == null || dto.getId() < 1) {
  		problem = "Missing user id!";
  	}
  	
  	if (problem == null && dto.getId() == CurrentUser.getUserId()) {
  		problem = "You cannot ban yourself!";
  	}

  	if (problem == null 
			&& (StringUtils.isBlank(dto.getText()) 
				|| dto.getText().length() < 5 || dto.getText().length() > 128)) {
  		problem = "Reason must be between 5 - 128 chars!";
  	}

  	return problem;
  }
  
}
