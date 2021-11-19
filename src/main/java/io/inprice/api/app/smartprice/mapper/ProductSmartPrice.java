package io.inprice.api.app.smartprice.mapper;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSmartPrice {

	//product
  private Integer actives = 0;
  private BigDecimal price = BigDecimal.ZERO;
  private BigDecimal basePrice = BigDecimal.ZERO;
  private BigDecimal minPrice = BigDecimal.ZERO;
  private BigDecimal avgPrice = BigDecimal.ZERO;
  private BigDecimal maxPrice = BigDecimal.ZERO;

  //smartprice
	private String formula;
	private String lowerLimitFormula;
	private String upperLimitFormula;
  
}
