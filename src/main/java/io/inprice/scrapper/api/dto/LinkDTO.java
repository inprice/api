package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LinkDTO implements Serializable {

  private static final long serialVersionUID = 4899893105959011844L;

  private String url;
  private Long productId;

}
