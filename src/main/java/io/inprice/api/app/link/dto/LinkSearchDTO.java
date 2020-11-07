package io.inprice.api.app.link.dto;

import io.inprice.api.dto.BaseSearchDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkSearchDTO extends BaseSearchDTO {

  private String[] statuses;

}
