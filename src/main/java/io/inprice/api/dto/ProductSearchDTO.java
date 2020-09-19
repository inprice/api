package io.inprice.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchDTO {

  private String term;
  private Long position;
  private Long brand;
  private Long category;
  private Boolean loadMore = Boolean.FALSE;
  private Integer rowCount = 0;

}
