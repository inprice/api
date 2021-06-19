package io.inprice.api.app.alarm.dto;

import java.util.Set;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.meta.OrderDir;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import io.inprice.common.meta.AlarmTopic;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

	private SearchBy searchBy = SearchBy.NAME;
	private AlarmTopic topic;
  private Set<AlarmSubject> subjects;
  private Set<AlarmSubjectWhen> whens;

  private OrderBy orderBy = OrderBy.NOTIFIED_AT;
  private OrderDir orderDir = OrderDir.DESC;

}
