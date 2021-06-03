package io.inprice.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TicketCommentDTO {

	private Long id;
  private String content;

  private Long ticketId;
  private Long userId;
  private Long accountId;

}
