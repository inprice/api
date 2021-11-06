package io.inprice.api.app.superuser.ticket;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
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
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;
import io.inprice.common.models.TicketHistory;
import io.inprice.common.utils.StringHelper;

/**
 * 
 * @since 2021-06-04
 * @author mdpinar
*/
public class TicketService {

  private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
  
  Response findById(Long id) {
  	try (Handle handle = Database.getHandle()) {
  		TicketDao ticketDao = handle.attach(TicketDao.class);
  		Ticket ticket = ticketDao.findById(id);
  		if (ticket != null) {
  			if (Boolean.FALSE.equals(ticket.getSeenBySuper())) {
  				ticket.setSeenBySuper(Boolean.TRUE);
  				ticketDao.toggleSeenBySuper(id, true);
  			}
  			return generateFullResponse(ticketDao, ticket);
  		}
  	}
  	return Responses.NotFound.TICKET;
  }
  
  Response changeStatus(ChangeStatusDTO dto) {
  	Response res = Responses.NotFound.TICKET;

  	if (dto.getId() != null && dto.getId() > 0) {
  		if (dto.getStatus() != null) {

    		try (Handle handle = Database.getHandle()) {
      		TicketDao ticketDao = handle.attach(TicketDao.class);
      		Ticket ticket = ticketDao.findById(dto.getId());

      		if (ticket != null) {
      			if (dto.getStatus().equals(ticket.getStatus()) == false) {
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
          	} else {
          		res = new Response("Ticket is already in " + dto.getStatus() + " status!");
          	}
      		}
      	}
    	} else {
    		res = new Response("Invalid status!");
    	}
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

    String workspaceOrdering = "";
    
    if (CurrentUser.hasSession()) {
      where.append("where workspace_id = ");
      where.append(CurrentUser.getWorkspaceId());
    } else {
      where.append("where 1=1 ");
      workspaceOrdering = "w.name, ";
    }

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and body like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (CollectionUtils.isNotEmpty(dto.getStatuses())) {
    	where.append(
		    String.format(" and t.status in (%s) ", StringHelper.join("'", dto.getStatuses()))
			);
    }

    if (CollectionUtils.isNotEmpty(dto.getPriorities())) {
    	where.append(
		    String.format(" and priority in (%s) ", StringHelper.join("'", dto.getPriorities()))
			);
    }

    if (CollectionUtils.isNotEmpty(dto.getTypes())) {
    	where.append(
		    String.format(" and type in (%s) ", StringHelper.join("'", dto.getTypes()))
			);
    }

    if (CollectionUtils.isNotEmpty(dto.getSubjects())) {
    	where.append(
		    String.format(" and subject in (%s) ", StringHelper.join("'", dto.getSubjects()))
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
          "select t.*, u.full_name, w.name as workspace from ticket t " +
      		"inner join user u on u.id = t.user_id " +
      		"inner join workspace w on w.id = t.workspace_id " +
          where +
          " order by " + workspaceOrdering + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() + ", t.id " +
          limit
        )
      .map(new TicketMapper())
      .list();

      return new Response(searchResult);
    } catch (Exception e) {
      logger.error("Failed in full search for tickets.", e);
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
