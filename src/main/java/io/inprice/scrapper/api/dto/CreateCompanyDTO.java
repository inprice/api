package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateCompanyDTO implements Serializable {

  private static final long serialVersionUID = -8983343002065096998L;

  private String name;
  private String currencyCode;
  private String currencyFormat;

}
