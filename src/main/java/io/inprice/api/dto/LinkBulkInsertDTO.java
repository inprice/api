package io.inprice.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LinkBulkInsertDTO {

  private Long groupId;
  private String linksText;
  private Boolean fromSearchPage = Boolean.FALSE;
  
}
