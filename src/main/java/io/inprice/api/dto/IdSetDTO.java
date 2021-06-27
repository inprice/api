package io.inprice.api.dto;

import java.io.Serializable;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IdSetDTO implements Serializable {

	private static final long serialVersionUID = 5206568143952756331L;

	private Set<Long> set;

}
