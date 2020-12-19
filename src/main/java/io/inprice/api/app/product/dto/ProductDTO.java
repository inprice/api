package io.inprice.api.app.product.dto;

import java.math.BigDecimal;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProductDTO {

  private Long id;
  private String code;
  private String name;
  private BigDecimal price;
  private Long importId;

  private Long accountId;
  private Set<String> tags;
  private Boolean tagsChanged = Boolean.FALSE;

}
