package io.inprice.api.app.ticket;

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
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;

/**
 * 
 * @since 2021-05-09
 * @author mdpinar
*/
public class TicketService {

  private static final Logger log = LoggerFactory.getLogger(TicketService.class);
  
  Response findById(Long id) {
  	try (Handle handle = Database.getHandle()) {
  		TicketDao ticketDao = handle.attach(TicketDao.class);
  		Ticket ticket = ticketDao.findById(id, CurrentUser.getAccountId());
  		if (ticket != null) {
  			if (Boolean.FALSE.equals(ticket.getSeenByUser())) {
  				ticketDao.toggleSeenByUser(id, true);
  			}
  			return generateFullResponse(ticketDao, ticket);
  		}
  	}
  	return Responses.NotFound.TICKET;
  }

	Response toggleSeenValue(Long id) {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			Ticket ticket = ticketDao.findById(id, CurrentUser.getAccountId());
			if (ticket != null) {
				if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
  				ticketDao.toggleSeenByUser(id, !ticket.getSeenByUser());
  				return Responses.OK;
				} else {
					return Responses.PermissionProblem.WRONG_USER;
				}
			}
		}
		return Responses.NotFound.TICKET;
	}

	Response insert(TicketDTO dto) {
		Response res = Responses.Invalid.TICKET;

		String problem = validate(dto);
		if (problem == null) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);
				handle.begin();
				
				long id = ticketDao.insert(dto);
				dto.setId(id);
				dto.setStatus(TicketStatus.OPENED);
				ticketDao.insertHistory(dto);
				res = Responses.OK;

				handle.commit();
			}
		} else {
			res = new Response(problem);
		}

		return res;
	}

	Response update(TicketDTO dto) {
		Response res = Responses.NotFound.TICKET;

		if (dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					Ticket ticket = ticketDao.findById(dto.getId(), dto.getAccountId());
					if (ticket != null) {
						if (TicketStatus.OPENED.equals(ticket.getStatus())) {
	  					if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {

	  						handle.begin();
	  						if (! ticket.getPriority().equals(dto.getPriority()) || ! ticket.getType().equals(dto.getType()) || ! ticket.getSubject().equals(dto.getSubject())) {
	  							ticketDao.insertHistory(dto);
	  						}

	  						boolean isOK = ticketDao.update(dto);
								if (isOK) {
									handle.commit();
									res = Responses.OK;
								} else {
									handle.rollback();
									res = Responses.DataProblem.DB_PROBLEM;
								}
	  					} else {
	  						res = Responses.PermissionProblem.WRONG_USER;
	  					}
						} else {
							res = Responses.NotAllowed.UPDATE;
						}
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}
	
	Response delete(Long id) {
		Response res = Responses.NotFound.TICKET;

		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);
				Ticket ticket = ticketDao.findById(id, CurrentUser.getAccountId());

				if (ticket != null) {
					if (TicketStatus.OPENED.equals(ticket.getStatus())) {
						if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
							
							handle.begin();
							ticketDao.deleteHistories(id);
							ticketDao.deleteComments(id);
							
							boolean isOK = ticketDao.delete(id);
							if (isOK) {
								handle.commit();
								res = Responses.OK;
							} else {
								handle.rollback();
								res = Responses.DataProblem.DB_PROBLEM;
							}
						} else {
							res = Responses.PermissionProblem.WRONG_USER;
						}
					} else {
						res = Responses.NotAllowed.UPDATE;
					}
				}
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

    where.append("where account_id = ");
    where.append(CurrentUser.getAccountId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and body like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (dto.getStatuses() != null && dto.getStatuses().size() > 0) {
    	where.append(
		    String.format(" and status in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getStatuses()))
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
    	where.append(" and seen_by_user = ");
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
          "select t.*, u.name as username from ticket t " +
      		"inner join user u on u.id= t.user_id " +
          where +
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
	
	private String validate(TicketDTO dto) {
		String problem = null;

		if (dto.getPriority() == null) {
			problem = "Priority cannot be empty!";
		}

		if (problem == null && dto.getType() == null) {
			problem = "Ticket type cannot be empty!";
		}

		if (problem == null && dto.getSubject() == null) {
			problem = "Subject cannot be empty!";
		}

		if (problem == null) {
  		if (StringUtils.isBlank(dto.getBody())) {
  			problem = "Issue cannot be empty!";
  		} else if (dto.getBody().length() < 12 || dto.getBody().length() > 1024) {
  			problem = "Issue must be between 12 - 1024 chars!";
  		}
		}

		if (problem == null) {
			dto.setUserId(CurrentUser.getUserId());
			dto.setAccountId(CurrentUser.getAccountId());
		}

		return problem;
	}
	
	private Response generateFullResponse(TicketDao dao, Ticket ticket) {
		List<TicketComment> commentList = dao.fetchCommentListByTicketId(ticket.getId());
		ticket.setCommentList(commentList);
		return new Response(ticket);
	}

}
