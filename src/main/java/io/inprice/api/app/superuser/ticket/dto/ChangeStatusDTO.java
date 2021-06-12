package io.inprice.api.app.superuser.ticket.dto;

import io.inprice.common.meta.TicketStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStatusDTO {

  private Long id;
  private TicketStatus status;

}
