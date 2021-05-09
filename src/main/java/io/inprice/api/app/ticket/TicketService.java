package io.inprice.api.app.ticket;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Ticket;

/**
 * 
 * @since 2021-09-05
 * @author mdpinar
*/
public class TicketService {

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
	
	Response search(String term) {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			List<Ticket> list = null;
			if (StringUtils.isNotBlank(term)) {
				list = ticketDao.search("%" + SqlHelper.clear(term) + "%", CurrentUser.getAccountId());
			} else {
				list = ticketDao.getList(CurrentUser.getAccountId());
			}
			return new Response(list);
		}
	}

	Response findUnreadList() {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			List<Ticket> list = ticketDao.findUnreadList(CurrentUser.getAccountId());
			return new Response(list);
		}
	}

	Response markAsRead(Long id) {
		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);
				boolean isOK = ticketDao.markAsRead(id, CurrentUser.getUserId());
				if (isOK) {
					return Responses.OK;
				} else {
					return Responses.PermissionProblem.WRONG_USER;
				}
			}
		}
		return Responses.NotFound.TICKET;
	}

	Response markAllAsRead() {
		try (Handle handle = Database.getHandle()) {
			TicketDao ticketDao = handle.attach(TicketDao.class);
			boolean isOK = ticketDao.markAllAsRead(CurrentUser.getUserId());
			if (isOK) return Responses.OK;
		}
		return Responses.NotFound.TICKET;
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
						return Responses.NotAllowed.DELETE;
					}
				} else {
					return Responses.NotFound.TICKET;
				}
			}
		}
		return Responses.Invalid.TICKET;
	}

	private String validate(TicketDTO dto) {
		String problem = null;

		if (StringUtils.isBlank(dto.getQuery())) {
			problem = "Text cannot be empty!";
		} else if (dto.getQuery().length() < 25 || dto.getQuery().length() > 1024) {
			problem = "Text must be between 25 and 1024 chars!";
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

}
