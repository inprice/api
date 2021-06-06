package io.inprice.api.app.ticket.dto;

import java.util.Set;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.meta.OrderDir;
import io.inprice.common.meta.TicketPriority;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.meta.TicketSubject;
import io.inprice.common.meta.TicketType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

	private SearchBy searchBy = SearchBy.BODY; //can be specified by only super users!
	private Set<TicketStatus> statuses;
  private Set<TicketPriority> priorities;
  private Set<TicketType> types;
  private Set<TicketSubject> subjects;
  private Seen seen = Seen.ALL;
  private OrderBy orderBy = OrderBy.PRIORITY;
  private OrderDir orderDir = OrderDir.DESC;

}
