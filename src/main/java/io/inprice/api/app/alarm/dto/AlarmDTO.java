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
	private String name;

  private AlarmTopic topic;
  private AlarmSubject subject;
  private AlarmSubjectWhen subjectWhen;

	private String certainPosition;
	private BigDecimal amountLowerLimit;
	private BigDecimal amountUpperLimit;

  @JsonIgnore
  private Long workspaceId;
	
}
