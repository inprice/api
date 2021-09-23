package io.inprice.api.app.product.dto;

import java.util.Set;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.meta.OrderDir;
import io.inprice.common.meta.Level;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

  private Set<Level> levels;
  private AlarmStatus alarmStatus = AlarmStatus.ALL;
  private OrderBy orderBy = OrderBy.NAME;
  private OrderDir orderDir = OrderDir.ASC;

}
