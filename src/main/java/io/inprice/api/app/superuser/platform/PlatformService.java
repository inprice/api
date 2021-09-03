package io.inprice.api.app.superuser.platform;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.superuser.platform.dto.PlatformDTO;
import io.inprice.api.app.superuser.platform.dto.SearchDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.PlatformMapper;
import io.inprice.common.models.Platform;

class PlatformService {

  private static final Logger logger = LoggerFactory.getLogger(PlatformService.class);

  Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, false);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder("where 1=1 ");

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and ");
  		where.append(dto.getSearchBy().getFieldName());
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (dto.getParked() != null) {
  		where.append(" and parked = ");
  		where.append(dto.getParked().toString());
    }

    if (dto.getBlocked() != null) {
  		where.append(" and blocked = ");
  		where.append(dto.getBlocked().toString());
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Platform> searchResult =
        handle.createQuery(
          "select * from platform " + 
          where +
          " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() +
          " limit " + dto.getRowCount() + ", " + dto.getRowLimit()
        )
      .map(new PlatformMapper())
      .list();
      
      return new Response(Map.of("rows", searchResult));
    } catch (Exception e) {
      logger.error("Failed in full search for platforms.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  Response update(PlatformDTO dto) {
  	Response res = Responses.NotFound.PLATFORM;

  	if (dto.getId() != null && dto.getId() > 0) {

      String problem = validate(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          PlatformDao platformDao = handle.attach(PlatformDao.class);
          boolean isUpdated = platformDao.update(dto);

          if (isUpdated) {
          	res = Responses.OK;
          } else {
          	res = Responses.DataProblem.DB_PROBLEM;
          }
        }

      } else {
      	res = new Response(problem);
      }
    }

  	return res;
  }

  Response toggleParked(Long id) {
    Response res = Responses.NotFound.PLATFORM;

  	if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        PlatformDao platformDao = handle.attach(PlatformDao.class);
        boolean isUpdated = platformDao.toggleParked(id);

        if (isUpdated) {
        	res = Responses.OK;
        } else {
        	res = Responses.DataProblem.DB_PROBLEM;
        }
      }
  	}

    return res;
  }

  Response toggleBlocked(Long id) {
    Response res = Responses.NotFound.PLATFORM;

  	if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        PlatformDao platformDao = handle.attach(PlatformDao.class);
        boolean isUpdated = platformDao.toggleBlocked(id);

        if (isUpdated) {
        	res = Responses.OK;
        } else {
        	res = Responses.DataProblem.DB_PROBLEM;
        }
      }
  	}

    return res;
  }
  
  private String validate(PlatformDTO dto) {
    String problem = null;

  	if (StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be empty!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 50) {
      problem = "Name must be between 3 - 50 chars!";
    }

    if (problem == null && (StringUtils.isBlank(dto.getCurrencyCode()) || dto.getCurrencyCode().length() != 3)) {
      problem = "Currency Code must be 3 chars!";
    }

    if (problem == null) {
    	if (StringUtils.isBlank(dto.getCurrencyFormat())) {
	      problem = "Currency Format cannot be empty!";
	    } else if (dto.getCurrencyFormat().length() < 3 || dto.getCurrencyFormat().length() > 30) {
	      problem = "Currency Format must be between 3 - 30 chars!";
	    }
    }

    if (problem == null) {
    	if (StringUtils.isBlank(dto.getQueue())) {
	      problem = "Queue cannot be empty!";
	    } else if (dto.getQueue().length() < 5 || dto.getQueue().length() > 50) {
	      problem = "Queue must be between 5 - 50 chars!";
	    }
    }

    if (problem == null && StringUtils.isNotBlank(dto.getProfile()) && (dto.getProfile().length() < 3 || dto.getProfile().length() > 15)) {
      problem = "If given, profile can be between 3 - 15 chars!";
    }

    return problem;
  }

}
