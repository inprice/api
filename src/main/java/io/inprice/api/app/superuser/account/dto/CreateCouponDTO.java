package io.inprice.api.app.superuser.account.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCouponDTO implements Serializable {

	private static final long serialVersionUID = -2320608884231408592L;

	private Integer planId;
	private Integer days;
  private String description;

  private Long accountId;

}
