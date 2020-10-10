package io.inprice.api.app.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchDTO {

  private String term;
  private Long position;
  private Boolean loadMore = Boolean.FALSE;
  private Integer rowCount = 0;
  private String[] selectedTags;

}