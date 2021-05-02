package io.inprice.api.app.link.dto;

import java.util.Set;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.common.meta.Level;
import io.inprice.common.meta.LinkStatusGroup;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

	//null means ALL
	private SearchBy searchBy = SearchBy.Name;
  private Set<Level> levels;
  private Set<LinkStatusGroup> statuses;
  private OrderBy orderBy = OrderBy.Name;
  private OrderDir orderDir = OrderDir.Ascending;

}
