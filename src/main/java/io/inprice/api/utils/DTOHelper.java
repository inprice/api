package io.inprice.api.utils;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.SqlHelper;

public class DTOHelper {
	
	public static <DTO extends BaseSearchDTO> DTO normalizeSearch(DTO dto, boolean accountIdAdded) {
		return normalizeSearch(dto, false, accountIdAdded);
	}
	
	public static <DTO extends BaseSearchDTO> DTO normalizeSearch(DTO dto, boolean percentageAdded, boolean accountIdAdded) {
		if (accountIdAdded) dto.setAccountId(CurrentUser.getAccountId());
		if (!dto.getLoadMore()) dto.setRowCount(0);
    if (dto.getRowLimit() < Consts.LOWER_ROW_LIMIT_FOR_LISTS) dto.setRowLimit(Consts.LOWER_ROW_LIMIT_FOR_LISTS);
    if (dto.getRowLimit() > Consts.UPPER_ROW_LIMIT_FOR_LISTS) dto.setRowLimit(Consts.UPPER_ROW_LIMIT_FOR_LISTS);
  	dto.setTerm(SqlHelper.clear(dto.getTerm()) + (percentageAdded ? "%" : ""));
  	return dto;
	}

}
