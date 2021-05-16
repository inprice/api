package io.inprice.api.app.superuser.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateCouponDTO implements Serializable {

	private static final long serialVersionUID = -2320608884231408592L;

	private Integer planId;
	private Integer days;
  private String description;

  private Long accountId;

}
