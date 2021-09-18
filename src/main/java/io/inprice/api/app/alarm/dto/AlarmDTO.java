package io.inprice.api.app.alarm.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import io.inprice.common.meta.AlarmTopic;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmDTO {

  private Long id;
  private AlarmTopic topic;
  private AlarmSubject subject;
  private AlarmSubjectWhen subjectWhen;

	private String certainStatus;
	private BigDecimal amountLowerLimit;
	private BigDecimal amountUpperLimit;

  private Long linkId;
  private Long productId;

  @JsonIgnore
  private Long accountId;
	
}
