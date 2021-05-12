package io.inprice.api.app.ticket;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.ticket.dto.ReplyStatus;
import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketCSatDTO;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.meta.TicketCSatLevel;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Link;
import io.inprice.common.models.Ticket;

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

			Ticket ticket = ticketDao.findById(id, CurrentUser.getAccountId());
			if (ticket != null) {
				return new Response(ticket);
			}
			return Responses.NotFound.TICKET;
		}
	}
	
	Response insert(TicketDTO dto) {
		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);
					boolean isOK = ticketDao.insert(dto);
					if (isOK)
						return Responses.OK;
				}
			} else {
				return new Response(problem);
			}
		}
		return Responses.Invalid.TICKET;
	}

	Response update(TicketDTO dto) {
		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					Ticket ticket = ticketDao.findById(dto.getId(), CurrentUser.getAccountId());
					if (ticket != null) {
					  if (ticket.getRepliedAt() == null) {
	  					if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
	  						boolean isOK = ticketDao.update(dto);
	  						if (isOK)
	  							return Responses.OK;
	  					} else {
	  						return Responses.PermissionProblem.WRONG_USER;
	  					}
						} else {
							return Responses.NotAllowed.UPDATE;
						}

					} else {
						return Responses.NotFound.TICKET;
					}
				}
			} else {
				return new Response(problem);
			}
		}
		return Responses.Invalid.TICKET;
	}

  public Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder criteria = new StringBuilder();

    criteria.append("where account_id = ");
    criteria.append(CurrentUser.getAccountId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	criteria.append(" and ");
    	criteria.append(dto.getSearchBy().getFieldName());
      criteria.append(" like '%");
      criteria.append(dto.getTerm());
      criteria.append("%'");
    }
    
    if (dto.getReplyStatus() != null && !dto.getReplyStatus().equals(ReplyStatus.ALL)) {
    	criteria.append(" and replied_at is ");
    	if (dto.getReplyStatus().equals(ReplyStatus.REPLIED)) {
    		criteria.append(" not ");
    	}
    	criteria.append(" null ");
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
          criteria +
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
	
	Response delete(Long id) {
		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);

				Ticket ticket = ticketDao.findById(id, CurrentUser.getAccountId());
				if (ticket != null) {
				  if (ticket.getRepliedAt() == null) {
  					if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
  						boolean isOK = ticketDao.delete(id, CurrentUser.getAccountId());
  						if (isOK)
  							return Responses.OK;
  					} else {
  						return Responses.PermissionProblem.WRONG_USER;
  					}
					} else {
						return Responses.NotAllowed.UPDATE;
					}
				} else {
					return Responses.NotFound.TICKET;
				}
			}
		}
		return Responses.Invalid.TICKET;
	}

	Response setSatisfaction(TicketCSatDTO dto) {
		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
    		try (Handle handle = Database.getHandle()) {
    			TicketDao ticketDao = handle.attach(TicketDao.class);
    
    			Ticket ticket = ticketDao.findById(dto.getId(), CurrentUser.getAccountId());
    			if (ticket != null) {
    			  if (ticket.getRepliedAt() != null && ticket.getCsatLevel() == null) {
    					if (CurrentUser.getRole().equals(UserRole.ADMIN) || ticket.getUserId().equals(CurrentUser.getUserId())) {
    						boolean isOK = ticketDao.setCSatLevel(dto.getId(), dto.getLevel(), dto.getReason());
    						if (isOK)
    							return Responses.OK;
    					} else {
    						return Responses.PermissionProblem.WRONG_USER;
    					}
    				} else {
    					return Responses.NotAllowed.UPDATE;
    				}
    			} else {
    				return Responses.NotFound.TICKET;
    			}
    		}
			} else {
				return new Response(problem);
			}
		}
		return Responses.Invalid.TICKET;
	}
	
	private String validate(TicketDTO dto) {
		String problem = null;
		
		if (problem == null) {
  		if (StringUtils.isBlank(dto.getQuery())) {
  			problem = "Text cannot be empty!";
  		} else if (dto.getQuery().length() < 12 || dto.getQuery().length() > 512) {
  			problem = "Text must be between 12-512 chars!";
  		}
		}

		if (problem == null && dto.getType() == null) {
			problem = "Ticket type cannot be empty!";
		}

		if (problem == null && dto.getSubject() == null) {
			problem = "Subject cannot be empty!";
		}

		if (problem == null) {
			dto.setUserId(CurrentUser.getUserId());
			dto.setAccountId(CurrentUser.getAccountId());
		}

		return problem;
	}
	
	private String validate(TicketCSatDTO dto) {
		String problem = null;
		
		if (dto.getId() == null || dto.getId() <= 0) {
			problem = "Ticket id is wrong!";
		}
		
		if (problem == null && dto.getLevel() == null) {
			problem = "Level cannot be empty!";
		}
		
		if (problem == null && dto.getLevel().ordinal() > TicketCSatLevel.GOOD.ordinal() && StringUtils.isBlank(dto.getReason())) {
			problem = "Please help us! " + dto.getLevel() + " level requires your assessment.";
		}
		
		return problem;
	}

}
