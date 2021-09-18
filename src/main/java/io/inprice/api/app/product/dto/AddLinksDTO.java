package io.inprice.api.app.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AddLinksDTO {

  private Long productId;
  private String linksText;
  
}
