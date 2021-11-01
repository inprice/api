package io.inprice.api.utils;

import io.inprice.api.consts.Consts;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.SqlHelper;

public class DTOHelper {

	public static <DTO extends BaseSearchDTO> DTO normalizeSearch(DTO dto, boolean willWorkspaceIdBeAdded) {
		return normalizeSearch(dto, false, willWorkspaceIdBeAdded);
	}

	public static <DTO extends BaseSearchDTO> DTO normalizeSearch(DTO dto, boolean willPercentageBeAdded, boolean willWorkspaceIdBeAdded) {
		if (willWorkspaceIdBeAdded) dto.setWorkspaceId(CurrentUser.getWorkspaceId());
		if (dto.getLoadMore() == false) dto.setRowCount(0);
    if (dto.getRowLimit() < Consts.LOWER_ROW_LIMIT_FOR_LISTS) dto.setRowLimit(Consts.LOWER_ROW_LIMIT_FOR_LISTS);
    if (dto.getRowLimit() > Consts.UPPER_ROW_LIMIT_FOR_LISTS) dto.setRowLimit(Consts.UPPER_ROW_LIMIT_FOR_LISTS);
    if (dto.getTerm() == null) dto.setTerm("");
  	dto.setTerm(SqlHelper.clear(dto.getTerm()) + (willPercentageBeAdded ? "%" : ""));
  	return dto;
	}

}
