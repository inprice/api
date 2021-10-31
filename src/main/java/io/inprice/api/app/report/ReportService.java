package io.inprice.api.app.report;

import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import io.inprice.api.app.report.info.ProductCriteriaDTO;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.SqlHelper;

/**
 * Generates reports
 * 
 * @since 2021-10-28
 * @author mdpinar
 */
public class ReportService extends ReportBase {

	Response generateProductReport(ProductCriteriaDTO dto, OutputStream outputStream) {
		StringBuilder sql = new StringBuilder();

    sql.append("and p.workspace_id = ");
    sql.append(CurrentUser.getWorkspaceId());

    if (dto.getAlarmStatus() != null && AlarmStatus.ALL.equals(dto.getAlarmStatus()) == false) {
  		sql.append(" and p.alarm_id is ");
    	if (AlarmStatus.ALARMED.equals(dto.getAlarmStatus())) {
    		sql.append(" not ");
    	}
    	sql.append(" null");
    }

    if (StringUtils.isNotBlank(dto.getSku())) {
  		sql.append(" and p.sku = '");
  		sql.append(SqlHelper.clear(dto.getSku()));
  		sql.append("'");
    }

    if (dto.getBrandId() != null) {
  		sql.append(" and p.brand_id = ");
  		sql.append(dto.getBrandId());
    }

    if (dto.getCategoryId() != null) {
  		sql.append(" and p.category_id = ");
  		sql.append(dto.getCategoryId());
    }

    if (CollectionUtils.isNotEmpty(dto.getPositions())) {
    	sql.append(
  			String.format(" and p.position in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getPositions()))
			);
    }
    
    Map<String, Object> extraParams = null;
    if (dto.getGroup() != null) {
    	extraParams = Map.of(
  			"GROUP_NAME", dto.getGroup().getLabel(),
  			"GROUP_FIELD", dto.getGroup().getField()
			);
    }

    return generate(dto.getSelectedReport(), dto.getReportUnit(), sql.toString(), outputStream, extraParams);
	}
	
}
