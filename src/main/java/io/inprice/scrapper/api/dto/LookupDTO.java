package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LookupDTO implements Serializable {

  private static final long serialVersionUID = 7924219666802285302L;

  private String type;
  private String newValue;

}
