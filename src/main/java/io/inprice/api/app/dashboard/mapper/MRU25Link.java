package io.inprice.api.app.dashboard.mapper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MRU25Link implements Serializable {
  
  private static final long serialVersionUID = 2062234416267369942L;

  private Long id;
  private Long productId;
  private String productName;
  private String seller;
  private String platform;
  private String name;
  private String url;
  private BigDecimal price;
  private String status;
  private String statusDesc;
  private String position;
  private Long alarmId;
  private String alarmName;
  private String updatedAt;
  private List<BigDecimal> prices = new ArrayList<>();

}
