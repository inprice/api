package io.inprice.api.app.dashboard.mapper;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MRU25Link implements Serializable {
  
  private static final long serialVersionUID = 2062234416267369942L;

  private String groupName;
  private String seller;
  private String platform;
  private String url;
  private BigDecimal price;
  private String status;
  private String level;
  private String updatedAt;

}
