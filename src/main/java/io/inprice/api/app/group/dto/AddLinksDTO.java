package io.inprice.api.app.group.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AddLinksDTO {

  private Long groupId;
  private String linksText;
  
}
