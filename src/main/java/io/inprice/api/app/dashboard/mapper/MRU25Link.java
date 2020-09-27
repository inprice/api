package io.inprice.api.app.dashboard.mapper;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MRU25Link {
  
  private String productName;
  private String seller;
  private BigDecimal price;
  private String status;
  private String lastUpdate;
  private String platform;

}
