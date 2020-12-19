package io.inprice.api.app.account.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateDTO implements Serializable {

  private static final long serialVersionUID = -8983343002065096998L;

  private String name;
  private String currencyCode;
  private String currencyFormat;

}
