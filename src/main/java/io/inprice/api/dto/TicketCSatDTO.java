package io.inprice.api.dto;

import java.io.Serializable;

import io.inprice.common.meta.TicketCSatLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TicketCSatDTO implements Serializable {

	private static final long serialVersionUID = -7281325596100885591L;

	private Long id;
	private TicketCSatLevel level;
  private String assessment;

}
