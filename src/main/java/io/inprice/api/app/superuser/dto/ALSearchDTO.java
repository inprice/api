package io.inprice.api.app.superuser.dto;

import java.util.Date;

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

	private Date startDate;
	private Date endDate;
	private ALMethod method;

	private Long userId;
	private Long accountId;

  private ALOrderBy orderBy = ALOrderBy.DATE;
  private ALOrderDir orderDir = ALOrderDir.DESC;
	
}
