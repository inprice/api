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
		Response res = Responses.Invalid.COMMENT;

		String problem = validate(dto);
		if (problem == null) {
			try (Handle handle = Database.getHandle()) {
				TicketDao ticketDao = handle.attach(TicketDao.class);

				Ticket ticket = ticketDao.findById(dto.getTicketId(), dto.getWorkspaceId());
				if (ticket != null) {
					if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
						handle.begin();
						ticketDao.makeAllCommentsNotEditable(dto.getTicketId());

						dto.setWorkspaceId(ticket.getWorkspaceId());
						boolean isOK = ticketDao.insertComment(dto);
						if (isOK) {
							ticketDao.incCommentCount(dto.getTicketId());
							handle.commit();
							List<TicketComment> commentList = ticketDao.fetchCommentListByTicketId(dto.getTicketId());
							res = new Response(commentList);
						} else {
							handle.rollback();
							res = Responses.DataProblem.DB_PROBLEM;
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

					Ticket ticket = ticketDao.findById(dto.getTicketId(), dto.getWorkspaceId());
					if (ticket != null) {
						if (! TicketStatus.CLOSED.equals(ticket.getStatus())) {
							TicketComment comment = ticketDao.findCommentById(dto.getId(), dto.getWorkspaceId());
							if (comment != null) {
								if (comment.getEditable()) {
			  					if (CurrentUser.getRole().equals(UserRole.ADMIN) || comment.getUserId().equals(CurrentUser.getUserId())) {

										dto.setWorkspaceId(ticket.getWorkspaceId());
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

				TicketComment comment = ticketDao.findCommentById(id, CurrentUser.getWorkspaceId());

				if (comment != null) {
					if (comment.getEditable()) {
  					Ticket ticket = ticketDao.findById(comment.getTicketId(), comment.getWorkspaceId());
  
  					if (ticket != null) {
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
  							res = Responses.NotAllowed.CLOSED_TICKET;
    					}
						} else {
  						res = Responses.NotFound.TICKET;
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
		} else if (dto.getBody().length() < 12 || dto.getBody().length() > 1024) {
			problem = "Body must be between 12 - 1024 chars!";
		}

		if (problem == null && dto.getTicketId() == null) {
			problem = "Ticket id cannot be empty!";
		}

		if (problem == null) {
			dto.setWorkspaceId(CurrentUser.getWorkspaceId());
			dto.setUserId(CurrentUser.getUserId());
		}

		return problem;
	}

}
