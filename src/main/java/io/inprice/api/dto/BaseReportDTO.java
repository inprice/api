package io.inprice.api.dto;

import io.inprice.api.meta.ReportUnit;
import io.inprice.api.meta.ReportType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BaseReportDTO {

	private ReportType selectedReport;
  private ReportUnit reportUnit = ReportUnit.Pdf;

}
