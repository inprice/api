package io.inprice.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseSearchDTO {

  private String term;
  private Boolean loadMore = Boolean.FALSE;
  private Integer rowCount = 0;

}
