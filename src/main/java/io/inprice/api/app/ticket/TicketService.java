package io.inprice.api.app.ticket;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.ticket.dto.SearchDTO;
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
import io.inprice.common.models.TicketReply;

/**
 * 
 * @since 2021-09-05
 * @author mdpinar
*/
public class TicketService {

  private static final Logger log = LoggerFactory.getLogger(TicketService.class);

	Response findById(Long id) {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			Ticket ticket = ticketDao.findById(id);
			if (ticket != null) {
				List<TicketReply> replyList = ticketDao.fetchReplyListByTicketId(id);
				ticket.setReplyList(replyList);
				return new Response(ticket);
			}
		}
		return Responses.NotFound.TICKET;
	}

	Response insert(TicketDTO dto) {
		Response res = Responses.Invalid.TICKET;

		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					if (dto.getTicketId() == null) { // is a ticket

						handle.begin();
						ticketDao.insertHistory(dto);
						
						boolean isOK = ticketDao.insert(dto);
						if (isOK) {
							handle.commit();
							res = Responses.OK;
						} else {
							handle.rollback();
							res = Responses.DataProblem.DB_PROBLEM;
						}
					} else { //is a reply
						Ticket parent = ticketDao.findById(dto.getTicketId());
						if (parent != null) {
							if (! TicketStatus.CLOSED.equals(parent.getStatus())) {

								handle.begin();
								ticketDao.makeAllPriorRepliesNotEditable(dto.getTicketId());
								
								boolean isOK = ticketDao.insertReply(dto);
								if (isOK) {
									res = Responses.OK;
								} else {
									res = Responses.DataProblem.DB_PROBLEM;
								}
							} else {
								res = new Response("Ticket has been Closed!");
							}
						}
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}

	Response update(TicketDTO dto) {
		Response res = Responses.Invalid.TICKET;

		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					if (dto.getTicketId() == null) { // is a ticket
						Ticket ticket = ticketDao.findById(dto.getId());
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
					} else { //is a reply
						Ticket parent = ticketDao.findById(dto.getId());
						if (parent != null) {
							if (! TicketStatus.CLOSED.equals(parent.getStatus())) {
								TicketReply reply = ticketDao.findReplyById(dto.getId());
								if (reply != null) {
									if (reply.getEditable()) {
				  					if (CurrentUser.getRole().equals(UserRole.ADMIN) || reply.getUserId().equals(CurrentUser.getUserId())) {
				  						boolean isOK = ticketDao.updateReply(dto);
  										if (isOK) {
  											res = Responses.OK;
  										} else {
  											res = Responses.DataProblem.DB_PROBLEM;
  										}
				  					} else {
				  						res = Responses.PermissionProblem.WRONG_USER;
				  					}
									} else {
										res = Responses.NotAllowed.UPDATE;
									}
								}
							} else {
								res = Responses.NotAllowed.UPDATE;
							}
						}
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}
	
	Response delete(boolean isTicket, Long id) {
		Response res = Responses.NotFound.TICKET;

		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);

				if (isTicket) { //is a ticket
					Ticket ticket = ticketDao.findById(id);

					if (ticket != null) {
						if (TicketStatus.OPENED.equals(ticket.getStatus())) {
							if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
								boolean isOK = ticketDao.delete(id);
								if (isOK) {
									res = Responses.OK;
								} else {
									res = Responses.DataProblem.DB_PROBLEM;
								}
							} else {
								res = Responses.PermissionProblem.WRONG_USER;
							}
						} else {
							res = Responses.NotAllowed.UPDATE;
						}
					}
				} else { //is a reply
					TicketReply reply = ticketDao.findReplyById(id);

					if (reply != null) {
						Ticket ticket = ticketDao.findById(id);

						if (TicketStatus.OPENED.equals(ticket.getStatus())) {
							if (CurrentUser.getRole().equals(UserRole.ADMIN) || reply.getUserId().equals(CurrentUser.getUserId())) {
								
								handle.begin();
								ticketDao.makeOnePreviousReplyEditable(reply.getTicketId(), id);

								boolean isOK = ticketDao.deleteReply(id);
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
		}

		return res;
	}

  public Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder crit = new StringBuilder();

    crit.append("where account_id = ");
    crit.append(CurrentUser.getAccountId());

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
          "select * from ticket " + 
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
	
	private String validate(TicketDTO dto) {
		String problem = null;
		
		if (StringUtils.isBlank(dto.getText())) {
			problem = "Issue cannot be empty!";
		} else if (dto.getText().length() < 12 || dto.getText().length() > 512) {
			problem = "Issue must be between 12-512 chars!";
		}

		if (problem == null) {
  		if (dto.getTicketId() == null) {

    		if (problem == null && dto.getPriority() == null) {
    			problem = "Priority type cannot be empty!";
    		}

    		if (problem == null && dto.getType() == null) {
    			problem = "Ticket type cannot be empty!";
    		}
    
    		if (problem == null && dto.getSubject() == null) {
    			problem = "Subject cannot be empty!";
    		}

  		} else if (dto.getTicketId() == null) {
  			problem = "Ticket not found!";
  		}
		}

		if (problem == null) {
			dto.setUserId(CurrentUser.getUserId());
			dto.setAccountId(CurrentUser.getAccountId());
		}

		return problem;
	}

}
