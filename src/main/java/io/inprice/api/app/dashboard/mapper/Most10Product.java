package io.inprice.api.app.dashboard.mapper;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Most10Product {
  
  private Long id;
  private String name;
  private BigDecimal price;
  private Integer links;
  private Integer ranking;
  private Integer rankingWith;
  private String lastUpdate;

}
