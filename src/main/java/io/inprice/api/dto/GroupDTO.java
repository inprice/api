package io.inprice.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class GroupDTO {

  private Long id;
  private String name;
  private BigDecimal price;
  
  private Long accountId;

}
