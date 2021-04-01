package io.inprice.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StringDTO implements Serializable {

  private static final long serialVersionUID = -6720720553359171076L;

  private String value;

}
