package io.inprice.api.app.superuser.ticket;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.app.ticket.dto.Seen;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;

/**
 * 
 * @since 2021-06-04
 * @author mdpinar
*/
public class TicketService {

  private static final Logger log = LoggerFactory.getLogger(TicketService.class);
  
  Response findById(Long id) {
  	try (Handle handle = Database.getHandle()) {
  		TicketDao ticketDao = handle.attach(TicketDao.class);
  		Ticket ticket = ticketDao.findById(id);
  		if (ticket != null) {
  			if (Boolean.FALSE.equals(ticket.getSeenBySuper())) {
  				ticketDao.toggleSeenBySuper(id, true);
  			}
  			return generateFullResponse(ticketDao, ticket);
  		}
  	}
  	return Responses.NotFound.TICKET;
  }

	Response toggleSeenValue(Long id) {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			Ticket ticket = ticketDao.findById(id);
			if (ticket != null) {
				if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
  				ticketDao.toggleSeenBySuper(id, !ticket.getSeenBySuper());
  				return Responses.OK;
				} else {
					return Responses.PermissionProblem.WRONG_USER;
				}
			}
		}
		return Responses.NotFound.TICKET;
	}
	
  public Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder crit = new StringBuilder();

    if (CurrentUser.hasSession()) {
      crit.append("where account_id = ");
      crit.append(CurrentUser.getAccountId());
    } else {
      crit.append("where 1=1 ");
    }

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	crit.append(" and ");
    	crit.append(dto.getSearchBy().getFieldName());
    	crit.append(" like '%");
      crit.append(dto.getTerm());
      crit.append("%'");
    }

    if (dto.getStatuses() != null && dto.getStatuses().size() > 0) {
    	crit.append(
		    String.format(" and status in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getStatuses()))
			);
    }

    if (dto.getPriorities() != null && dto.getPriorities().size() > 0) {
    	crit.append(
		    String.format(" and priority in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getPriorities()))
			);
    }

    if (dto.getTypes() != null && dto.getTypes().size() > 0) {
    	crit.append(
		    String.format(" and type in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getTypes()))
			);
    }

    if (dto.getSubjects() != null && dto.getSubjects().size() > 0) {
    	crit.append(
		    String.format(" and subject in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getSubjects()))
			);
    }

    if (dto.getSeen() != null && !Seen.ALL.equals(dto.getSeen()) ) {
    	crit.append(" and seen_by_super = ");
    	if (Seen.SEEN.equals(dto.getSeen())) {
    		crit.append(" true ");
    	} else {
    		crit.append(" false ");
    	}
    }

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
      List<Ticket> searchResult =
        handle.createQuery(
          "select *, u.name as username, a.name as account from ticket t " +
      		"inner join user u on u.id=t.user_id " +
      		"inner join account a on a.id=t.account_id " +
          crit +
          " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() +
          limit
        )
      .map(new TicketMapper())
      .list();

      return new Response(Collections.singletonMap("rows", searchResult));
    } catch (Exception e) {
      log.error("Failed in full search for tickets.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }
	
	private Response generateFullResponse(TicketDao dao, Ticket ticket) {
		List<TicketComment> commentList = dao.fetchCommentListByTicketId(ticket.getId());
		ticket.setCommentList(commentList);
		return new Response(ticket);
	}

}
