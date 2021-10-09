package io.inprice.api.app.superuser.ticket;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketCommentDTO;
import io.inprice.api.dto.TicketDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;

/**
 * 
 * @since 2021-06-03
 * @author mdpinar
*/
public class CommentService {

	Response insert(TicketCommentDTO dto) {
		Response res = Responses.NotFound.TICKET;

		String problem = validate(dto);
		if (problem == null) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);

				Ticket ticket = ticketDao.findById(dto.getTicketId());
				if (ticket != null) {
					if (TicketStatus.CLOSED.equals(ticket.getStatus()) == false) {
						handle.begin();
						ticketDao.makeAllCommentsNotEditable(dto.getTicketId());

						dto.setWorkspaceId(ticket.getWorkspaceId());
						boolean isOK = ticketDao.insertComment(dto);
						if (isOK) {
							ticketDao.increaseCommentCount(dto.getTicketId());

							if (dto.getTicketNewStatus() != null && ticket.getStatus().equals(dto.getTicketNewStatus()) == false) {
								ticket.setStatus(dto.getTicketNewStatus());
								isOK = ticketDao.changeStatus(ticket.getId(), ticket.getStatus());
								if (isOK) {
									isOK = ticketDao.insertHistory(new TicketDTO(ticket));
								}
							}
							if (isOK) {
  							handle.commit();
  							List<TicketComment> commentList = ticketDao.fetchCommentListByTicketId(dto.getTicketId());
  							res = new Response(commentList);
							}
						}

						if (isOK == false){
							handle.rollback();
							res = Responses.NotSuitable.TICKET;
						}
					} else {
						res = Responses.NotAllowed.CLOSED_TICKET;
					}
				} else {
					res = Responses.NotFound.TICKET;
				}
			}
		} else {
			res = new Response(problem);
		}

		return res;
	}

	Response update(TicketCommentDTO dto) {
		Response res = Responses.NotFound.COMMENT;

		if (dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					Ticket ticket = ticketDao.findById(dto.getTicketId());
					if (ticket != null) {
						if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
							TicketComment comment = ticketDao.findCommentById(dto.getId());
							if (comment != null) {
								if (comment.getEditable()) {
									dto.setWorkspaceId(ticket.getWorkspaceId());
		  						boolean isOK = ticketDao.updateComment(dto);
									if (isOK) {
										List<TicketComment> commentList = ticketDao.fetchCommentListByTicketId(dto.getTicketId());
										res = new Response(commentList);
									} else {
										res = Responses.NotSuitable.TICKET;
									}
								} else {
									res = Responses.NotAllowed.UPDATE;
								}
							}
						} else {
							res = Responses.NotAllowed.CLOSED_TICKET;
						}
					} else {
						res = Responses.NotFound.TICKET;
					}
				}
			} else {
				res = new Response(problem);
			}
		}

		return res;
	}
	
	Response delete(Long id) {
		Response res = Responses.NotFound.COMMENT;

		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);

				TicketComment comment = ticketDao.findCommentById(id);

				if (comment != null) {
					if (comment.getEditable()) {
  					Ticket ticket = ticketDao.findById(comment.getTicketId());
  
  					if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
							handle.begin();
							ticketDao.makeOnePreviousCommentEditable(comment.getTicketId(), id);

							boolean isOK = ticketDao.deleteCommentById(id);
							if (isOK) {
								ticketDao.decreaseCommentCount(comment.getTicketId());
								handle.commit();
								List<TicketComment> commentList = ticketDao.fetchCommentListByTicketId(comment.getTicketId());
								res = new Response(commentList);
							} else {
								handle.rollback();
								res = Responses.DataProblem.DB_PROBLEM;
							}

  					} else {
  						res = Responses.NotAllowed.CLOSED_TICKET;
						}
					} else {
						res = Responses.NotAllowed.UPDATE;
					}
				}
			}
		}

		return res;
	}
	
	private String validate(TicketCommentDTO dto) {
		String problem = null;
		
		if (StringUtils.isBlank(dto.getBody())) {
			problem = "Body cannot be empty!";
		} else if (dto.getBody().length() < 12) {
			problem = "Body cannot be shorter than 12 chars!";
		}

		if (problem == null && dto.getTicketId() == null) {
			problem = "Ticket not found!";
		}

		if (problem == null) {
			dto.setUserId(CurrentUser.getUserId());
		}

		return problem;
	}

}
