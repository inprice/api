package io.inprice.api.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class GroupDTO {

  private Long id;
  private String name;
  private String description;
  private BigDecimal price;
  
  private Long accountId;

}
