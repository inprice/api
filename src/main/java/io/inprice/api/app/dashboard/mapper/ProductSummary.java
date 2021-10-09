package io.inprice.api.app.dashboard.mapper;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSummary implements Serializable {
  
  private static final long serialVersionUID = -1701445492576224844L;

  private Long id;
  private String sku;
  private String name;
  private Integer actives;
  private Integer waitings;
  private Integer tryings;
  private Integer problems;
  private Integer total;
  private BigDecimal price;
  private String updatedAt;
  
}
