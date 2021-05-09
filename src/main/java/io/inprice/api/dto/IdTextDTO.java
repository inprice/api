package io.inprice.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IdTextDTO implements Serializable {

	private static final long serialVersionUID = 3413078382324926419L;

	private Long id;
  private String text;

}
