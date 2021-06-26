package io.inprice.api.app.superuser.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.inprice.api.dto.BaseSearchDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * Search dto class for AccessLog table
 * 
 * @author mdpinar
 */
@Getter
@Setter
public class ALSearchDTO extends BaseSearchDTO {

	private ALSearchBy searchBy = ALSearchBy.PATH;

  @JsonFormat(pattern="yyyy-MM-dd", timezone=JsonFormat.DEFAULT_TIMEZONE)
	private Date startDate;

  @JsonFormat(pattern="yyyy-MM-dd", timezone=JsonFormat.DEFAULT_TIMEZONE)
  private Date endDate;

  private ALMethod method;
	private Long userId;

  private ALOrderBy orderBy = ALOrderBy.DATE;
  private ALOrderDir orderDir = ALOrderDir.DESC;
	
}
