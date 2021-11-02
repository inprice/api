package io.inprice.api.app.report;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private static final ReportService service = Beans.getSingleton(ReportService.class);
  
  @Override
  public void addRoutes(Javalin app) {

    app.get(Consts.Paths.Report.PRODUCT, (ctx) -> {
    	Response dtoConvRes = toProductCriteriaDTO(ctx.queryParamMap());
    	if (dtoConvRes.isOK()) {
    		ProductCriteriaDTO dto = dtoConvRes.getData();
	  		Response res = service.generateProductReport(dto, ctx.res.getOutputStream());
	  		if (res.isOK()) {
	  			ctx
	  				.contentType(dto.getReportUnit().getContentType())
	  				.header("Content-Disposition", "attachment; filename=" + dto.getSelectedReport().name().replaceAll("_", "") + dto.getReportUnit().getFileExtention());
	  		} else {
	  			ctx.status(400).json(res);
	  		}
    	} else {
    		ctx.status(400).json(dtoConvRes);
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

    app.get(Consts.Paths.Report.LINK, (ctx) -> {
    	Response dtoConvRes = toLinkCriteriaDTO(ctx.queryParamMap());
    	if (dtoConvRes.isOK()) {
    		LinkCriteriaDTO dto = dtoConvRes.getData();
	  		Response res = service.generateLinkReport(dto, ctx.res.getOutputStream());
	  		if (res.isOK()) {
	  			ctx
	  				.contentType(dto.getReportUnit().getContentType())
	  				.header("Content-Disposition", "attachment; filename=" + dto.getSelectedReport().name().replaceAll("_", "") + dto.getReportUnit().getFileExtention());
	  		} else {
	  			ctx.status(400).json(res);
	  		}
    	} else {
    		ctx.status(400).json(dtoConvRes);
    	}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

  }

  private Response toProductCriteriaDTO(Map<String, List<String>> queryParams) {
  	if (queryParams.size() == 0) {
  		return Responses.REQUEST_BODY_INVALID;
  	}

  	ProductCriteriaDTO dto = new ProductCriteriaDTO();

  	try {
  		dto.setSelectedReport(ReportType.valueOf(queryParams.get("selectedReport").get(0)));
  	} catch (Exception e) {
  		return new Response("Please selected a report!");
  	}

  	if (queryParams.get("sku") != null) {
  		dto.setSku(queryParams.get("sku").get(0));
  	}

  	try {
	  	if (queryParams.get("positions") != null) {
	  		List<String> positions = queryParams.get("positions");
	  		Set<Position> set = new HashSet<>(positions.size());
	  		for (String pos: positions) {
	  			set.add(Position.valueOf(pos));
	  		}
	  		dto.setPositions(set);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid link position!");
  	}
	
  	try {
	  	if (queryParams.get("alarmStatus") != null) {
	  		AlarmStatus as = AlarmStatus.valueOf(queryParams.get("alarmStatus").get(0));
	  		dto.setAlarmStatus(as);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid alarm status!");
  	}
	
  	try {
	  	if (queryParams.get("brandId") != null) {
	  		long id = Long.valueOf(queryParams.get("brandId").get(0));
	  		if (id > 0)
	  			dto.setBrandId(id);
	  		else
	  			throw new Exception("");
	  	}
		} catch (Exception e) {
			return new Response("Invalid brand!");
		}
	
  	try {
	  	if (queryParams.get("categoryId") != null) {
	  		long id = Long.valueOf(queryParams.get("categoryId").get(0));
	  		if (id > 0)
		  		dto.setCategoryId(id);
	  		else
	  			throw new Exception("");
	  	}
		} catch (Exception e) {
			return new Response("Invalid category!");
		}

  	try {
	  	if (queryParams.get("group") != null) {
	  		ProductGroup gr = ProductGroup.valueOf(queryParams.get("group").get(0));
	  		dto.setGroup(gr);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid product group!");
  	}

	  try {
	  	if (queryParams.get("reportUnit") != null) {
	  		ReportUnit ru = ReportUnit.valueOf(queryParams.get("reportUnit").get(0));
	  		dto.setReportUnit(ru);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid report unit!");
  	}

  	return new Response(dto);
  }

  private Response toLinkCriteriaDTO(Map<String, List<String>> queryParams) {
  	if (queryParams.size() == 0) {
  		return Responses.REQUEST_BODY_INVALID;
  	}

  	LinkCriteriaDTO dto = new LinkCriteriaDTO();

  	try {
  		dto.setSelectedReport(ReportType.valueOf(queryParams.get("selectedReport").get(0)));
  	} catch (Exception e) {
  		return new Response("Please selected a report!");
  	}

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

  	try {
	  	if (queryParams.get("grups") != null) {
	  		List<String> grups = queryParams.get("grups");
	  		Set<Grup> set = new HashSet<>(grups.size());
	  		for (String gr: grups) {
	  			set.add(Grup.valueOf(gr));
	  		}
	  		dto.setGrups(set);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid link status!");
  	}

  	try {
	  	if (queryParams.get("positions") != null) {
	  		List<String> positions = queryParams.get("positions");
	  		Set<Position> set = new HashSet<>(positions.size());
	  		for (String pos: positions) {
	  			set.add(Position.valueOf(pos));
	  		}
	  		dto.setPositions(set);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid link position!");
  	}
	
  	try {
	  	if (queryParams.get("alarmStatus") != null) {
	  		AlarmStatus as = AlarmStatus.valueOf(queryParams.get("alarmStatus").get(0));
	  		dto.setAlarmStatus(as);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid alarm status!");
  	}

  	try {
	  	if (queryParams.get("reportUnit") != null) {
	  		ReportUnit ru = ReportUnit.valueOf(queryParams.get("reportUnit").get(0));
	  		dto.setReportUnit(ru);
	  	}
  	} catch (Exception e) {
  		return new Response("Invalid report unit!");
  	}

  	return new Response(dto);
  }

}
