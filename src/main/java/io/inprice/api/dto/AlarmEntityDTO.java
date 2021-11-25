package io.inprice.api.dto;

import java.io.Serializable;
import java.util.Set;

import io.inprice.common.meta.AlarmTopic;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmEntityDTO implements Serializable {

	private static final long serialVersionUID = -8427892709482345999L;

	private AlarmTopic topic;
	private Long alarmId;
	private Set<Long> entityIdSet; //used for representing link or product ids

}
