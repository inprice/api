package io.inprice.api.app.superuser.ticket.dto;

import io.inprice.common.meta.TicketStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChangeStatusDTO {

  private Long id;
  private TicketStatus status;

}
