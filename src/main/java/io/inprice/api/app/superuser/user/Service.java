package io.inprice.api.app.superuser.user;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.superuser.account.Dao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.User;

class Service {

	Response search(BaseSearchDTO dto) {
  	try (Handle handle = Database.getHandle()) {
    	Dao superDao = handle.attach(Dao.class);
    	return new Response(superDao.search(DTOHelper.normalizeSearch(dto)));
    }
	}
  
  Response ban(IdTextDTO dto) {
  	String problem = null;
  	
  	if (dto.getId() == CurrentUser.getUserId()) {
  		problem = "Invalid user!";
  	}
  	if (problem == null 
  			&& (StringUtils.isBlank(dto.getText()) 
  					|| dto.getText().length() < 3 || dto.getText().length() > 128)) {
  		problem = "Reason must be between 3-128 chars!";
  	}
  	
  	if (problem == null) {
  		try (Handle handle = Database.getHandle()) {
  			UserDao userDao = handle.attach(UserDao.class);
  			User user = userDao.findById(dto.getId());
  			if (user != null) {
  				if (user.isBanned()) {
  					Dao superDao = handle.attach(Dao.class);
  					boolean isOK = superDao.ban(dto.getId(), dto.getText());
  					if (isOK) {
  						return Responses.OK;
  					} else {
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
      		if (! user.isBanned()) {
      			Dao superDao = handle.attach(Dao.class);
      			boolean isOK = superDao.revokeBan(id);
      			if (isOK) {
      				return Responses.OK;
      			} else {
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
  
}
