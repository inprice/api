package io.inprice.api.app.superuser.platform.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformDTO implements Serializable {

	private static final long serialVersionUID = 4092356413564396973L;

	private Long id;
  private String name;
  private String currencyCode;
  private String currencyFormat;
  private String queue;
  private String profile;
  private Boolean parked = Boolean.FALSE;
  private Boolean blocked = Boolean.FALSE;

}
