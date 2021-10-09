package io.inprice.api.dto;

import io.inprice.api.session.CurrentUser;
import io.inprice.common.meta.TicketPriority;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.meta.TicketSubject;
import io.inprice.common.meta.TicketType;
import io.inprice.common.models.Ticket;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class TicketDTO {

	private Long id;
	private TicketStatus status;
	private TicketPriority priority;
	private TicketType type;
  private TicketSubject subject;
  private String body;
  private Long userId;
  private Long workspaceId;

  //clones ticket for inserting history table
  public TicketDTO(Ticket ticket) {
  	this.id = ticket.getId();
  	this.status = ticket.getStatus();
  	this.priority = ticket.getPriority();
  	this.type = ticket.getType();
  	this.subject = ticket.getSubject();
  	this.body = ticket.getBody();
  	this.workspaceId = ticket.getWorkspaceId();

  	this.userId = CurrentUser.getUserId();
  }
  
}
