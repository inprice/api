package io.inprice.api.app.dashboard.mapper;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Most10Product implements Serializable {
  
  private static final long serialVersionUID = -1701445492576224844L;

  private Long id;
  private String name;
  private BigDecimal price;
  private Integer linkCount;
  private Integer ranking;
  private Integer rankingWith;
  private String lastUpdate;

}
