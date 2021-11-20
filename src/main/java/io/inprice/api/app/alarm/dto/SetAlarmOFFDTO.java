package io.inprice.api.app.alarm.dto;

import java.io.Serializable;
import java.util.Set;

import io.inprice.common.meta.AlarmTopic;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetAlarmOFFDTO implements Serializable {

	private static final long serialVersionUID = -5248011303107436488L;

	private AlarmTopic alarmTopic;
	private Set<Long> entityIdSet; //used for representing link or product ids

}
