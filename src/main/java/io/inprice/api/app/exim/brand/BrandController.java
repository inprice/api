package io.inprice.api.app.exim.brand;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.AbstractController;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;
import io.javalin.http.Context;

@Router
public class BrandController extends AbstractController {

  private static final BrandService service = Beans.getSingleton(BrandService.class);

  private static final String UPLOAD = "/upload";
  private static final String DOWNLOAD = "/download";
  
  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Exim.BRAND + UPLOAD, (ctx) -> {
    	Response checkRes = checkTheFile(ctx);
    	if (checkRes.isOK()) {
	  		ctx.json(service.upload(checkRes.getData()));
    	} else {
    		ctx.json(checkRes);
    	}
    }, AccessRoles.EDITOR());

    app.get(Consts.Paths.Exim.BRAND + DOWNLOAD, (ctx) -> {
  		Response res = service.download(ctx.res.getOutputStream());
  		if (res.isOK()) {
  			ctx
  				.contentType("text/csv")
  				.header("Content-Disposition", "attachment; filename=Brands.csv");
  		} else {
  			ctx.status(400).json(res);
  		}
    }, AccessRoles.ANYONE_PLUS_SUPER_WITH_WORKSPACE());

  }

  private Response checkTheFile(Context ctx) {
  	return checkTheFile(ctx, 1_048_576); //1mb
  }
  
  private Response checkTheFile(Context ctx, int bodyMaxLimit) {
    String body = ctx.body().trim();

  	if (StringUtils.isBlank(body)) {
  		return Responses.REQUEST_BODY_INVALID;
  	} else if ("text/csv".equals(ctx.req.getContentType()) == false) {
  		return Responses.Invalid.FILE_TYPE;
  	} else if (body.length() > bodyMaxLimit) {
  		return Responses.Invalid.FILE_LENGTH_TOO_LARGE;
  	} else {
  		return Response.dataAsString(body);
  	}
  }

}
