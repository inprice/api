package io.inprice.api.app.announce;

import java.util.Collections;
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

/**
 * 
 * @since 2021-06-05
 * @author mdpinar
*/
public class AnnounceService {

  private static final Logger log = LoggerFactory.getLogger(AnnounceService.class);

	Response fetchNewAnnounces() {
		try (Handle handle = Database.getHandle()) {
			AnnounceDao announceDao = handle.attach(AnnounceDao.class);
			List<Announce> list = announceDao.fetchNotLoggedAnnounces(CurrentUser.getUserId(), CurrentUser.getAccountId());
			return new Response(list);
		}
	}
	
	Response addLogsForCurrentUser() {
		try (Handle handle = Database.getHandle()) {
			AnnounceDao announceDao = handle.attach(AnnounceDao.class);
			announceDao.addLogsForWaitingAnnounces(CurrentUser.getUserId(), CurrentUser.getAccountId());
			return Responses.OK;
		}
	}

  Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder("where 1=1 ");

    if (CollectionUtils.isNotEmpty(dto.getTypes())) {
    	boolean hasSYSTEM = dto.getTypes().contains(AnnounceType.SYSTEM);
    	boolean hasACCOUNT = dto.getTypes().contains(AnnounceType.ACCOUNT);
    	boolean hasUSER = dto.getTypes().contains(AnnounceType.USER);
    	
    	if (hasSYSTEM) where.append(" and type='SYSTEM' ");

    	if (hasACCOUNT) {
    		if (hasSYSTEM) 
    			where.append(" or ");
    		else
    			where.append(" and ");
    		where.append(" (type='ACCOUNT' and account_id=");
    		where.append(CurrentUser.getAccountId());
    		where.append(") ");
    	}

    	if (hasUSER) {
    		if (hasSYSTEM || hasACCOUNT) 
    			where.append(" or ");
    		else
    			where.append(" and ");
    		where.append(" (type='USER' and user_id=");
    		where.append(CurrentUser.getUserId());
    		where.append(") ");
    	}
    }
    
    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and ");
    	where.append(dto.getSearchBy().getFieldName());
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
      log.error("Failed in full search for announces.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

}
