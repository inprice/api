package io.inprice.api.app.alarm.dto;

import java.math.BigDecimal;

import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmDTO {

  private Long id;
  private String forWhich;
  private AlarmSubject subject;
  private AlarmSubjectWhen subjectWhen;

	private String certainStatus;
	private BigDecimal priceLowerLimit;
	private BigDecimal priceUpperLimit;

  private Long linkId;
  private Long groupId;

  private Long accountId;
	
}
