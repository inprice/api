package io.inprice.api.app.report.info;

import java.util.Set;

import io.inprice.api.dto.BaseReportDTO;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.common.meta.Position;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCriteriaDTO extends BaseReportDTO {

	private String sku;
  private Set<Position> positions;
  private AlarmStatus alarmStatus = AlarmStatus.ALL;
  private Long brandId;
  private Long categoryId;

  private Group group;

}
