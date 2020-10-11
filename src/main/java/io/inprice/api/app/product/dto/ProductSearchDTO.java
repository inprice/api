package io.inprice.api.app.product.dto;

import io.inprice.api.dto.BaseSearchDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchDTO extends BaseSearchDTO {

  private Long position;
  private String[] selectedTags;

}
