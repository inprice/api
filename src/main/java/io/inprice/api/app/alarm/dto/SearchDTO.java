package io.inprice.api.app.alarm.dto;

import java.util.Set;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.meta.OrderDir;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

	private ForWhich forWhich = ForWhich.ALL;
  private Set<AlarmSubject> subjects;
  private Set<AlarmSubjectWhen> whens;

  private OrderBy orderBy = OrderBy.TRIGGERED_AT;
  private OrderDir orderDir = OrderDir.DESC;

}
