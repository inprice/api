package io.inprice.api.app.report;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.inprice.api.app.report.info.Group;
import io.inprice.api.app.report.info.ProductCriteriaDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.meta.ReportUnit;
import io.inprice.api.meta.SelectedReport;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.Position;
import io.javalin.Javalin;

@Router
public class ReportController extends AbstractController {

  private static final ReportService service = Beans.getSingleton(ReportService.class);
  
  @Override
  public void addRoutes(Javalin app) {
  	
    app.get(Consts.Paths.Report.PRODUCT, (ctx) -> {
    	ProductCriteriaDTO dto = toProductPricesDTO(ctx.queryParamMap());
    	if (dto != null) {
	  		Response res = service.generateProductReport(dto, ctx.res.getOutputStream());
	  		if (res.isOK()) {
	  			ctx
	  				.contentType(dto.getReportUnit().getContentType())
	  				.header("Content-Disposition", "attachment; filename=" + dto.getSelectedReport().name().replaceAll("_", "") + dto.getReportUnit().getFileExtention());
	  		} else {
	  			ctx.status(400).json(res);
	  		}
    	} else {
    		ctx.status(400).json(Responses.REQUEST_BODY_INVALID);
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

  }
  
  private ProductCriteriaDTO toProductPricesDTO(Map<String, List<String>> queryParams) {
  	if (queryParams.size() == 0 || queryParams.get("selectedReport") == null) return null;

  	ProductCriteriaDTO dto = new ProductCriteriaDTO();

  	try {
  		dto.setSelectedReport(SelectedReport.valueOf(queryParams.get("selectedReport").get(0)));

	  	if (queryParams.get("positions") != null) {
	  		List<String> positions = queryParams.get("positions");
	  		Set<Position> set = new HashSet<>(positions.size());
	  		for (String pos: positions) {
	  			set.add(Position.valueOf(pos));
	  		}
	  		dto.setPositions(set);
	  	}
	
	  	if (queryParams.get("alarmStatus") != null) {
	  		AlarmStatus as = AlarmStatus.valueOf(queryParams.get("alarmStatus").get(0));
	  		dto.setAlarmStatus(as);
	  	}
	
	  	if (queryParams.get("brandId") != null) {
	  		long id = Long.valueOf(queryParams.get("brandId").get(0));
	  		dto.setBrandId(id);
	  	}
	
	  	if (queryParams.get("categoryId") != null) {
	  		long id = Long.valueOf(queryParams.get("categoryId").get(0));
	  		dto.setCategoryId(id);
	  	}

	  	if (queryParams.get("group") != null) {
	  		Group gr = Group.valueOf(queryParams.get("group").get(0));
	  		dto.setGroup(gr);
	  	}

	  	if (queryParams.get("reportUnit") != null) {
	  		ReportUnit ru = ReportUnit.valueOf(queryParams.get("reportUnit").get(0));
	  		dto.setReportUnit(ru);
	  	}
  	} catch (Exception e) {
  		return null;
  	}

  	return dto;
  }

}
