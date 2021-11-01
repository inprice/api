package io.inprice.api.app.report;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.report.info.LinkCriteriaDTO;
import io.inprice.api.app.report.info.ProductCriteriaDTO;
import io.inprice.api.app.report.info.ProductGroup;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.meta.ReportType;
import io.inprice.api.meta.ReportUnit;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.Grup;
import io.inprice.common.meta.Position;
import io.javalin.Javalin;

@Router
public class ReportController extends AbstractController {

  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  private static final ReportService service = Beans.getSingleton(ReportService.class);
  
  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Report.PRODUCT, (ctx) -> {
    	ProductCriteriaDTO dto = toProductCriteriaDTO(ctx.queryParamMap());
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

    app.get(Consts.Paths.Report.LINK, (ctx) -> {
    	LinkCriteriaDTO dto = toLinkCriteriaDTO(ctx.queryParamMap());
    	if (dto != null) {
	  		Response res = service.generateLinkReport(dto, ctx.res.getOutputStream());
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

  private ProductCriteriaDTO toProductCriteriaDTO(Map<String, List<String>> queryParams) {
  	if (queryParams.size() == 0 || queryParams.get("selectedReport") == null) return null;

  	ProductCriteriaDTO dto = new ProductCriteriaDTO();

  	try {
  		dto.setSelectedReport(ReportType.valueOf(queryParams.get("selectedReport").get(0)));

	  	if (queryParams.get("sku") != null) {
	  		dto.setSku(queryParams.get("sku").get(0));
	  	}

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
	  		ProductGroup gr = ProductGroup.valueOf(queryParams.get("group").get(0));
	  		dto.setGroup(gr);
	  	}

	  	if (queryParams.get("reportUnit") != null) {
	  		ReportUnit ru = ReportUnit.valueOf(queryParams.get("reportUnit").get(0));
	  		dto.setReportUnit(ru);
	  	}
  	} catch (Exception e) {
  		logger.warn("Failed to convert product report query params", e);
  		return null;
  	}

  	return dto;
  }

  private LinkCriteriaDTO toLinkCriteriaDTO(Map<String, List<String>> queryParams) {
  	if (queryParams.size() == 0 || queryParams.get("selectedReport") == null) return null;

  	LinkCriteriaDTO dto = new LinkCriteriaDTO();

  	try {
  		dto.setSelectedReport(ReportType.valueOf(queryParams.get("selectedReport").get(0)));

	  	if (queryParams.get("sku") != null) {
	  		dto.setSku(queryParams.get("sku").get(0));
	  	}

	  	if (queryParams.get("brand") != null) {
	  		dto.setBrand(queryParams.get("brand").get(0));
	  	}

	  	if (queryParams.get("seller") != null) {
	  		dto.setSeller(queryParams.get("seller").get(0));
	  	}

	  	if (queryParams.get("platform") != null) {
	  		dto.setPlatform(queryParams.get("platform").get(0));
	  	}

	  	if (queryParams.get("grups") != null) {
	  		List<String> grups = queryParams.get("grups");
	  		Set<Grup> set = new HashSet<>(grups.size());
	  		for (String gr: grups) {
	  			set.add(Grup.valueOf(gr));
	  		}
	  		dto.setGrups(set);
	  	}

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

	  	if (queryParams.get("reportUnit") != null) {
	  		ReportUnit ru = ReportUnit.valueOf(queryParams.get("reportUnit").get(0));
	  		dto.setReportUnit(ru);
	  	}
  	} catch (Exception e) {
  		logger.warn("Failed to convert link report query params", e);
  		return null;
  	}

  	return dto;
  }

}
