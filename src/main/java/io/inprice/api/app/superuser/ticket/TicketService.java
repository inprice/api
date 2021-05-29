package io.inprice.api.app.superuser.ticket;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.ticket.dto.SolveStatus;
import io.inprice.api.app.ticket.dto.SearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.meta.UserRole;
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
			return new Response(ticketDao.findWholeById(id));
		}
	}

	Response insert(TicketDTO dto) {
		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					handle.begin();
					ticketDao.makeAllPrecedingsNotUpdatable(dto.getParentId());

					boolean isOK = ticketDao.insert(dto);
					if (isOK) {
						handle.commit();
						return Responses.OK;
					}

					handle.rollback();
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

					Ticket ticket = ticketDao.findById(dto.getId());
					if (ticket != null) {
						if (Boolean.TRUE.equals(ticket.getUpdatable())) {
	  					if (CurrentUser.getRole().equals(UserRole.SUPER)) {
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
	
	Response delete(Long id) {
		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);

				Ticket ticket = ticketDao.findById(id);
				if (ticket != null) {
					if (Boolean.TRUE.equals(ticket.getUpdatable())) {
						boolean isOK = ticketDao.delete(id);
						if (isOK) {
							return Responses.OK;
  					} else {
  						return Responses.DataProblem.DB_PROBLEM;
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

  public Response search(SearchDTO dto) {
  	if (dto.getTerm() != null) dto.setTerm(SqlHelper.clear(dto.getTerm()));

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder crit = new StringBuilder();

    crit.append("where parent_id is null ");

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	crit.append(" and ");
    	crit.append(dto.getSearchBy().getFieldName());
      crit.append(" like '%");
      crit.append(dto.getTerm());
      crit.append("%'");
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

    if (dto.getSolveStatus() != null && !dto.getSolveStatus().equals(SolveStatus.ALL)) {
    	if (dto.getSolveStatus().equals(SolveStatus.SOLVED)) {
      	crit.append(" and solved = true ");
    	} else {
      	crit.append(" and solved = false ");
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
			problem = "Reply cannot be empty!";
		} else if (dto.getText().length() < 12 || dto.getText().length() > 1024) {
			problem = "Reply must be between 12-1024 chars!";
		}

		if (problem == null && dto.getParentId() == null) {
			problem = "Super user not allowed to open a ticket!";
		}
		
		if (problem == null) {
			if (dto.getId() != null && Boolean.FALSE.equals(dto.getUpdatable())) {
				problem = "This ticket cannot be changed!";
			}
		}

		return problem;
	}

}
