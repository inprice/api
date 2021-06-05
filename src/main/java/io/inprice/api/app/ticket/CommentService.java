package io.inprice.api.app.ticket;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.TicketCommentDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;

/**
 * 
 * @since 2021-06-03
 * @author mdpinar
*/
public class CommentService {

	Response insert(TicketCommentDTO dto) {
		Response res = Responses.Invalid.TICKET;

		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					Ticket ticket = ticketDao.findById(dto.getTicketId(), dto.getAccountId());
					if (ticket != null) {
						if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
							handle.begin();
							ticketDao.makeAllCommentsNotEditable(dto.getTicketId());

							dto.setAccountId(ticket.getAccountId());
							boolean isOK = ticketDao.insertComment(dto);
							if (isOK) {
  							ticketDao.increaseCommentCount(dto.getTicketId());
  							handle.commit();
  							List<TicketComment> commentList = ticketDao.fetchCommentListByTicketId(dto.getTicketId());
  							res = new Response(commentList);
							} else {
								handle.rollback();
								res = Responses.DataProblem.DB_PROBLEM;
							}
						} else {
							res = new Response("You are not allowed to add comment to a closed ticket!");
						}
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}

	Response update(TicketCommentDTO dto) {
		Response res = Responses.Invalid.TICKET;

		if (dto != null && dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					TicketDao ticketDao = handle.attach(TicketDao.class);

					Ticket ticket = ticketDao.findById(dto.getTicketId(), dto.getAccountId());
					if (ticket != null) {
						if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
							TicketComment comment = ticketDao.findCommentById(dto.getId());
							if (comment != null) {
								if (comment.getEditable()) {
			  					if (CurrentUser.getRole().equals(UserRole.ADMIN) || comment.getUserId().equals(CurrentUser.getUserId())) {

										dto.setAccountId(ticket.getAccountId());
			  						boolean isOK = ticketDao.updateComment(dto);
										if (isOK) {
											List<TicketComment> commentList = ticketDao.fetchCommentListByTicketId(dto.getTicketId());
											res = new Response(commentList);
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
							res = new Response("You are not allowed to update comment on a closed ticket!");
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

				TicketComment comment = ticketDao.findCommentById(id);

				if (comment != null) {
					if (comment.getEditable()) {
  					Ticket ticket = ticketDao.findById(comment.getTicketId(), comment.getAccountId());
  
  					if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
  						if (CurrentUser.getRole().equals(UserRole.ADMIN) || comment.getUserId().equals(CurrentUser.getUserId())) {
  							
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
  							res = Responses.PermissionProblem.WRONG_USER;
  						}
						} else {
							res = new Response("You are not allowed to delete comment from a closed ticket!");
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
		
		if (StringUtils.isBlank(dto.getContent())) {
			problem = "Content cannot be empty!";
		} else if (dto.getContent().length() < 12 || dto.getContent().length() > 512) {
			problem = "Content must be between 12-512 chars!";
		}

		if (problem == null && dto.getTicketId() == null) {
			problem = "Ticket not found!";
		}

		if (problem == null) {
			dto.setAccountId(CurrentUser.getAccountId());
			dto.setUserId(CurrentUser.getUserId());
		}

		return problem;
	}

}
