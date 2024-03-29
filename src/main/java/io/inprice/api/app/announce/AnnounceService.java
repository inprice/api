package io.inprice.api.app.announce;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.announce.dto.OrderBy;
import io.inprice.api.app.announce.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.AnnounceMapper;
import io.inprice.common.meta.AnnounceType;
import io.inprice.common.models.Announce;
import io.inprice.common.utils.DateUtils;
import io.inprice.common.utils.StringHelper;

/**
 * 
 * @since 2021-06-05
 * @author mdpinar
*/
public class AnnounceService {

  private static final Logger logger = LoggerFactory.getLogger(AnnounceService.class);

	Response fetchNewAnnounces() {
		try (Handle handle = Database.getHandle()) {
			AnnounceDao announceDao = handle.attach(AnnounceDao.class);
			List<Announce> list = announceDao.fetchNotLoggedAnnounces(CurrentUser.getUserId(), CurrentUser.getWorkspaceId());
			return new Response(list);
		}
	}
	
	Response addLogsForCurrentUser() {
		try (Handle handle = Database.getHandle()) {
			AnnounceDao announceDao = handle.attach(AnnounceDao.class);
			announceDao.addLogsForWaitingAnnounces(CurrentUser.getUserId(), CurrentUser.getWorkspaceId());
			return Responses.OK;
		}
	}
	
	Response addLogForCurrentUser(Long announceId) {
		if (announceId != null && announceId > 0) {
			try (Handle handle = Database.getHandle()) {
				AnnounceDao announceDao = handle.attach(AnnounceDao.class);
				announceDao.markAsRead(announceId, CurrentUser.getUserId(), CurrentUser.getWorkspaceId());
				List<Announce> list = announceDao.fetchNotLoggedAnnounces(CurrentUser.getUserId(), CurrentUser.getWorkspaceId());
				return new Response(list);
			}
		}
		return Responses.NotFound.ANNOUNCE;
	}

  Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder("where 1=1 ");

    if (CollectionUtils.isNotEmpty(dto.getTypes())) {
    	boolean hasSYSTEM = dto.getTypes().contains(AnnounceType.SYSTEM);
    	boolean hasWORKSPACE = dto.getTypes().contains(AnnounceType.WORKSPACE);
    	boolean hasUSER = dto.getTypes().contains(AnnounceType.USER);
    	
    	if (hasSYSTEM) where.append(" and type='SYSTEM' ");

    	if (hasWORKSPACE) {
    		if (hasSYSTEM) 
    			where.append(" or ");
    		else
    			where.append(" and ");
    		where.append(" (type='WORKSPACE' and workspace_id=");
    		where.append(CurrentUser.getWorkspaceId());
    		where.append(") ");
    	}

    	if (hasUSER) {
    		if (hasSYSTEM || hasWORKSPACE) 
    			where.append(" or ");
    		else
    			where.append(" and ");
    		where.append(" (type='USER' and user_id=");
    		where.append(CurrentUser.getUserId());
    		where.append(") ");
    	}

    } else {
			where.append(" and (type='SYSTEM' ");
			where.append(" or workspace_id=");
			where.append(CurrentUser.getWorkspaceId());
			where.append(" or user_id=");
			where.append(CurrentUser.getUserId());
			where.append(") ");
  	}
    
    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and CONCAT(title, body)");
    	where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }
    
    if (dto.getStartingAt() != null) {
    	where.append(" and starting_at>=");
    	where.append(DateUtils.formatDateForDB(dto.getStartingAt()));
    }

    if (dto.getEndingAt() != null) {
    	where.append(" and ending_at<=");
    	where.append(DateUtils.formatDateForDB(dto.getEndingAt()));
    }

    if (CollectionUtils.isNotEmpty(dto.getLevels())) {
    	where.append(
		    String.format(" and level in (%s) ", StringHelper.join("'", dto.getLevels()))
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

      return new Response(searchResult);
    } catch (Exception e) {
      logger.error("Failed in full search for announces.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

}
