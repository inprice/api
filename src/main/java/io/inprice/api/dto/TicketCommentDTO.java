package io.inprice.api.dto;

import io.inprice.common.meta.TicketStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TicketCommentDTO {

	private Long id;
  private String body;
  private Long ticketId;

  private Long userId; //not coming from client side, instead set programmatically!
  private Long accountId; //not coming from client side, instead set programmatically!

  private TicketStatus ticketNewStatus; //can be set only by super user!
  
}
