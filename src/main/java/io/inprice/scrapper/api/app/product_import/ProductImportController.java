package io.inprice.scrapper.api.app.product_import;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.framework.Router;
import io.inprice.scrapper.api.helpers.AccessRoles;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.meta.ImportType;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

@Router
public class ProductImportController implements Controller {

  private static final Logger log = LoggerFactory.getLogger(ProductImportController.class);

  private static final ProductImportService importService = Beans.getSingleton(ProductImportService.class);
  private static final ProductCSVImportService csvImportService = Beans.getSingleton(ProductCSVImportService.class);
  private static final ProductCodeImportService codeImportService = Beans.getSingleton(ProductCodeImportService.class);

  @Override
  public void addRoutes(Javalin app) {

    // find
    app.get(Consts.Paths.Product.IMPORT_BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, importService.findById(id)));
    }, AccessRoles.ANYONE());

    // search
    app.get(Consts.Paths.Product.IMPORTED_PRODUCTS, (ctx) -> {
      Map<String, String> searchMap = ControllerHelper.editSearchMap(ctx.queryParamMap());
      ctx.json(Commons.createResponse(ctx, importService.search(searchMap)));
    }, AccessRoles.ANYONE());

    // delete
    app.delete(Consts.Paths.Product.IMPORT_BASE + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, importService.deleteById(id)));
    }, AccessRoles.EDITOR());

    // upload CSV
    app.post(Consts.Paths.Product.IMPORT_CSV, (ctx) -> {
      upload(ctx, "text/csv", csvImportService);
    }, AccessRoles.EDITOR());

    // upload ebay SKU list
    app.post(Consts.Paths.Product.IMPORT_EBAY_SKU, (ctx) -> {
      upload(ctx, "text/plain", codeImportService, ImportType.EBAY_SKU);
    }, AccessRoles.EDITOR());

    // upload amazon ASIN list
    app.post(Consts.Paths.Product.IMPORT_AMAZON_ASIN, (ctx) -> {
      upload(ctx, "text/plain", codeImportService, ImportType.AMAZON_ASIN);
    }, AccessRoles.EDITOR());

  }

  private void upload(Context ctx, String contentType, IProductImportService importService) {
    upload(ctx, contentType, importService, null);
  }

  private void upload(Context ctx, String contentType, IProductImportService importService, ImportType importType) {
    UploadedFile file = ctx.uploadedFile("file");
    if (file != null && file.getSize() >= 32) { // byte
      if (file.getContentType().equals(contentType)) {
        try {
          String content = IOUtils.toString(file.getContent(), StandardCharsets.UTF_8.name());
          ServiceResponse result = (importType == null ? importService.upload(content)
              : importService.upload(importType, content));
          ctx.json(Commons.createResponse(ctx, result));
        } catch (IOException e) {
          log.error("Failed to upload content", e);
          ctx.json(Commons.createResponse(ctx, Responses.ServerProblem.FAILED));
        }
      } else {
        if ("text/csv".equals(contentType))
          ctx.json(Commons.createResponse(ctx, Responses.Upload.MUST_BE_CSV));
        else
          ctx.json(Commons.createResponse(ctx, Responses.Upload.MUST_BE_TXT));
      }
    } else {
      ctx.json(Commons.createResponse(ctx, Responses.Upload.EMPTY));
    }
  }

}
