package io.inprice.scrapper.api.app.product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductPrice implements Serializable {

   private static final long serialVersionUID = 5103850978461831401L;

   private Long id;
   private Long productId;
   private BigDecimal price;
   private BigDecimal avgPrice;
   private Integer position;
   private String minPlatform;
   private String minSeller;
   private BigDecimal minPrice;
   private String maxPlatform;
   private String maxSeller;
   private BigDecimal maxPrice;
   private Long companyId;
   private Date createdAt;

}
