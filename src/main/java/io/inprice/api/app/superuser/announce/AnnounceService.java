package io.inprice.api.app.superuser.announce;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.announce.dto.AnnounceDTO;
import io.inprice.api.app.announce.dto.OrderBy;
import io.inprice.api.app.announce.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.AnnounceMapper;
import io.inprice.common.meta.AnnounceLevel;
import io.inprice.common.meta.AnnounceType;
import io.inprice.common.models.Announce;

/**
 * 
 * @since 2021-06-05
 * @author mdpinar
*/
public class AnnounceService {

  private static final Logger logger = LoggerFactory.getLogger(AnnounceService.class);

	Response insert(AnnounceDTO dto) {
		Response res = Responses.NotFound.ANNOUNCE;

		String problem = validate(dto);
		if (problem == null) {
			try (Handle handle = Database.getHandle()) {
				AnnounceDao announceDao = handle.attach(AnnounceDao.class);
				boolean isOK = announceDao.insert(dto);
				if (isOK) res = Responses.OK;
			}
		} else {
			res = new Response(problem);
		}

		return res;
	}

	Response update(AnnounceDTO dto) {
		Response res = Responses.NotFound.ANNOUNCE;

		if (dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					AnnounceDao announceDao = handle.attach(AnnounceDao.class);
					boolean isOK = announceDao.update(dto);
					if (isOK) res = Responses.OK;
				}
			} else {
				res = new Response(problem);
			}
		} else {
			res = new Response("Announce id is missing!");
		}
		
		return res;
	}

	Response delete(Long id) {
		Response res = Responses.NotFound.ANNOUNCE;
		
		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				AnnounceDao announceDao = handle.attach(AnnounceDao.class);
				boolean isOK = announceDao.delete(id);
				if (isOK) res = Responses.OK;
			}
		}
		
		return res;
	}

  Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder();
    where.append("where 1=1 ");

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and ");
    	where.append(dto.getSearchBy().getFieldName());
    	where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }
    
    if (dto.getStartingAt() != null) {
    	where.append(" and starting_at>=");
    	where.append(io.inprice.common.utils.DateUtils.formatDateForDB(dto.getStartingAt()));
    }

    if (dto.getEndingAt() != null) {
    	where.append(" and ending_at<=");
    	where.append(io.inprice.common.utils.DateUtils.formatDateForDB(dto.getEndingAt()));
    }

    if (CollectionUtils.isNotEmpty(dto.getTypes())) {
    	where.append(
		    String.format(" and type in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getTypes()))
			);
    }

    if (CollectionUtils.isNotEmpty(dto.getLevels())) {
    	where.append(
		    String.format(" and level in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getLevels()))
			);
    }

    String orderBy = dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir();
    if (OrderBy.TITLE.equals(dto.getOrderBy()) || OrderBy.LEVEL.equals(dto.getOrderBy()) || OrderBy.TYPE.equals(dto.getOrderBy())) {
    	orderBy = "created_at desc, " + orderBy;
    }
    orderBy = "order by " + orderBy;

    //limiting
    String limit = "";
    if (dto.getRowLimit() < Consts.LOWER_ROW_LIMIT_FOR_LISTS && dto.getRowLimit() <= Consts.UPPER_ROW_LIMIT_FOR_LISTS) {
    	dto.setRowLimit(Consts.LOWER_ROW_LIMIT_FOR_LISTS);
    }
    if (dto.getRowLimit() > Consts.UPPER_ROW_LIMIT_FOR_LISTS) {
    	dto.setRowLimit(Consts.UPPER_ROW_LIMIT_FOR_LISTS);
    }
    if (dto.getLoadMore()) {
      limit = " limit " + dto.getRowCount() + ", " + dto.getRowLimit();
    } else {
    	limit = " limit " + dto.getRowLimit();
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Announce> searchResult =
        handle.createQuery(
          "select * from announce " +
          where +
          orderBy + ", id " +
          limit
        )
      .map(new AnnounceMapper())
      .list();

      return new Response(Collections.singletonMap("rows", searchResult));
    } catch (Exception e) {
      logger.error("Failed in full search for announces.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }
	
	private String validate(AnnounceDTO dto) {
		String problem = null;
		
		if (StringUtils.isBlank(dto.getTitle())) {
			problem = "Title cannot be empty!";
		} else if (dto.getTitle().length() < 5 || dto.getTitle().length() > 50) {
			problem = "Title must be between 5 - 50 chars!";
		}

		if (problem == null) {
  		if (StringUtils.isBlank(dto.getBody())) {
  			problem = "Body cannot be empty!";
  		} else if (dto.getBody().length() < 12) {
  			problem = "Body must be at least 11 chars!";
  		}
		}

		if (problem == null && dto.getLevel() == null) {
			problem = "Level cannot be empty!";
		}
		
		dto.setType(AnnounceType.SYSTEM);

		if (dto.getUserId() != null) dto.setType(AnnounceType.USER);
		if (dto.getAccountId() != null) dto.setType(AnnounceType.ACCOUNT);

		return problem;
	}
	
	public void createWelcomeMsg(Handle handle, Long userId) {
    try {
      Date today = new Date();
      AnnounceDao announceDao = handle.attach(AnnounceDao.class);
      AnnounceDTO dto = new AnnounceDTO();
      dto.setUserId(userId);
      dto.setLevel(AnnounceLevel.INFO);
      dto.setType(AnnounceType.USER);
      dto.setStartingAt(today);
      dto.setEndingAt(DateUtils.addDays(today, 3));
      dto.setTitle("Welcome on board!");
      String body = IOUtils.toString(this.getClass().getResourceAsStream("/announces/welcome.html"), "UTF-8");
      dto.setBody(body);
      announceDao.insert(dto);
    } catch (Exception e) {
      logger.error("Failed to read welcome announcement template!", e);
    }
	}

}
