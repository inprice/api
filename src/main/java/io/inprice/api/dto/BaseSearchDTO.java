package io.inprice.api.dto;

import io.inprice.api.consts.Consts;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BaseSearchDTO {

  private String term;
  private Integer rowCount = 0;
  private Integer rowLimit = Consts.LOWER_ROW_LIMIT_FOR_LISTS;
  private Boolean loadMore = Boolean.FALSE;

  private Long accountId;

}
