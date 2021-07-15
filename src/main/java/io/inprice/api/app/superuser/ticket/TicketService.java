package io.inprice.api.app.superuser.ticket;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.superuser.ticket.dto.ChangeStatusDTO;
import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.app.ticket.dto.Seen;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;
import io.inprice.common.models.TicketHistory;

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
  
  Response changeStatus(ChangeStatusDTO dto) {
  	Response res = Responses.NotFound.TICKET;

  	if (dto.getId() != null && dto.getId().longValue() > 0 && dto.getStatus() != null) {
    	if (! TicketStatus.OPENED.equals(dto.getStatus())) {

    		try (Handle handle = Database.getHandle()) {
      		TicketDao ticketDao = handle.attach(TicketDao.class);
      		Ticket ticket = ticketDao.findById(dto.getId());

      		if (ticket != null) {
      			handle.begin();

						ticket.setStatus(dto.getStatus());
						boolean isOK = ticketDao.changeStatus(ticket.getId(), ticket.getStatus());
						if (isOK) {
							isOK = ticketDao.insertHistory(new TicketDTO(ticket));
						}

    				if (isOK) {
							handle.commit();
    					res = Responses.OK;
    				} else {
    					handle.rollback();
    					res = Responses.DataProblem.DB_PROBLEM;
    				}
      		}
      	}
    	} else {
    		res = new Response("Changing a ticket's status to OPENED is not allowed!");
    	}
  	} else {
  		res = Responses.BAD_REQUEST;
  	}

  	return res;
  }

	Response toggleSeenValue(Long id) {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			Ticket ticket = ticketDao.findById(id);
			if (ticket != null) {
				boolean isOK = ticketDao.toggleSeenBySuper(id, !ticket.getSeenBySuper());
				if (isOK) {
					return Responses.OK;
				} else {
					return Responses.DataProblem.DB_PROBLEM;
				}
			}
		}
		return Responses.NotFound.TICKET;
	}

  Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder();

    String accountOrdering = "";
    
    if (CurrentUser.hasSession()) {
      where.append("where account_id = ");
      where.append(CurrentUser.getAccountId());
    } else {
      where.append("where 1=1 ");
      accountOrdering = "a.name, ";
    }

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and ");
    	where.append(dto.getSearchBy().getFieldName());
    	where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (dto.getStatuses() != null && dto.getStatuses().size() > 0) {
    	where.append(
		    String.format(" and t.status in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getStatuses()))
			);
    }

    if (dto.getPriorities() != null && dto.getPriorities().size() > 0) {
    	where.append(
		    String.format(" and priority in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getPriorities()))
			);
    }

    if (dto.getTypes() != null && dto.getTypes().size() > 0) {
    	where.append(
		    String.format(" and type in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getTypes()))
			);
    }

    if (dto.getSubjects() != null && dto.getSubjects().size() > 0) {
    	where.append(
		    String.format(" and subject in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getSubjects()))
			);
    }

    if (dto.getSeen() != null && !Seen.ALL.equals(dto.getSeen()) ) {
    	where.append(" and seen_by_super = ");
    	if (Seen.SEEN.equals(dto.getSeen())) {
    		where.append(" true ");
    	} else {
    		where.append(" false ");
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
          "select t.*, u.name as username, a.name as account from ticket t " +
      		"inner join user u on u.id = t.user_id " +
      		"inner join account a on a.id = t.account_id " +
          where +
          " order by " + accountOrdering + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() +
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
		List<TicketHistory> historyList = dao.fetchHistoryListByTicketId(ticket.getId());
		List<TicketComment> commentList = dao.fetchCommentListByTicketId(ticket.getId());
		ticket.setHistoryList(historyList);
		ticket.setCommentList(commentList);
		return new Response(ticket);
	}

}
