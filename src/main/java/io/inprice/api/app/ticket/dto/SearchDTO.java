package io.inprice.api.app.ticket.dto;

import io.inprice.api.dto.BaseSearchDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

	private SearchBy searchBy = SearchBy.QUERY;
  private ReplyStatus replyStatus;
  private OrderBy orderBy = OrderBy.CREATED_AT;
  private OrderDir orderDir = OrderDir.DESC;

}
