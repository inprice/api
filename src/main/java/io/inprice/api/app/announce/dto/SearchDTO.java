package io.inprice.api.app.announce.dto;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.meta.OrderDir;
import io.inprice.common.meta.AnnounceLevel;
import io.inprice.common.meta.AnnounceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDTO extends BaseSearchDTO {

  private Set<AnnounceType> types;
  private Set<AnnounceLevel> levels;

  @JsonFormat(pattern="yyyy-MM-dd HH:mm")
  private Date startingAt;

  @JsonFormat(pattern="yyyy-MM-dd HH:mm")
  private Date endingAt;
  
  private OrderBy orderBy = OrderBy.CREATED_AT;
  private OrderDir orderDir = OrderDir.DESC;

}
