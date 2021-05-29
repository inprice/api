package io.inprice.api.app.ticket.dto;

import java.util.Set;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.common.meta.TicketPriority;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.meta.TicketSubject;
import io.inprice.common.meta.TicketType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

	private SearchBy searchBy = SearchBy.ISSUE;
	private Set<TicketStatus> statuses;
  private Set<TicketPriority> priorities;
  private Set<TicketType> types;
  private Set<TicketSubject> subjects;
  private OrderBy orderBy = OrderBy.CREATED_AT;
  private OrderDir orderDir = OrderDir.DESC;

}
