package io.inprice.api.dto;

import io.inprice.common.meta.TicketPriority;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.meta.TicketSubject;
import io.inprice.common.meta.TicketType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TicketDTO {

	private Long id;
	private TicketStatus status;
	private TicketPriority priority;
	private TicketType type;
  private TicketSubject subject;
  private String issue;
  private Long userId;
  private Long accountId;

}
