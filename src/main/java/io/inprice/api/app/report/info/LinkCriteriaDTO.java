package io.inprice.api.app.report.info;

import java.util.Set;

import io.inprice.api.dto.BaseReportDTO;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.common.meta.Grup;
import io.inprice.common.meta.Position;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkCriteriaDTO extends BaseReportDTO {

	private String sku;
	private String brand;
	private String seller;
	private String platform;
	private Set<Grup> grups;
  private Set<Position> positions;
  private AlarmStatus alarmStatus = AlarmStatus.ALL;

}
