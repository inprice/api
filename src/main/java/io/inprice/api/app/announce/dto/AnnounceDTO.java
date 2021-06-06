package io.inprice.api.app.announce.dto;

import java.util.Date;

import io.inprice.common.meta.AnnounceLevel;
import io.inprice.common.meta.AnnounceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnounceDTO {

  private Long id;
  private AnnounceType type;
  private AnnounceLevel level;
  private String title;
  private String body;
  private String link;
  private Date startingAt;
  private Date endingAt;
	
}
