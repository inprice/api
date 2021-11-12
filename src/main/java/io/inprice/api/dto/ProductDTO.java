package io.inprice.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDTO {

  private Long id;
  private String sku;
  private String name;
  private BigDecimal price;
  private BigDecimal basePrice;

  private SimpleDef brand;
  private SimpleDef category;

  private Long smartPriceId;  //set by user
  private BigDecimal suggestedPrice; //set by the system in update section
  private BigDecimal suggestedPriceProblem; //set by the system in update section

  private Long brandId;    //for dao
  private Long categoryId; //for dao

  private Long workspaceId;
  
  private String from; //to distinguish the source of the request

}
