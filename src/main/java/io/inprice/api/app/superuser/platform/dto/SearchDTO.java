package io.inprice.api.app.superuser.platform.dto;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.meta.OrderDir;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

  private Boolean parked;
  private Boolean blocked;
  private OrderBy orderBy = OrderBy.NAME;
  private OrderDir orderDir = OrderDir.ASC;

}
