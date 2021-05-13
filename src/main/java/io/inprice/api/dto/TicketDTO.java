package io.inprice.api.dto;

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
	private TicketType type;
  private TicketSubject subject;
  private String query;
  private Long linkId;
  private Long groupId;
  
  private Long userId;
  private Long accountId;

}
