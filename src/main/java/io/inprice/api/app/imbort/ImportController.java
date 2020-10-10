package io.inprice.api.app.imbort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.AccessRoles;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.ImportType;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

@Router
public class ImportController implements Controller {

  private static final Logger log = LoggerFactory.getLogger(ImportController.class);

  private static final CSVImportService csvImportService = Beans.getSingleton(CSVImportService.class);
  private static final GenericImportService genericImportService = Beans.getSingleton(GenericImportService.class);

  @Override
  public void addRoutes(Javalin app) {

    // upload CSV
    app.post(Consts.Paths.Product.IMPORT_CSV, (ctx) -> {
      upload(ctx, "text/csv", csvImportService);
    }, AccessRoles.EDITOR());

    // upload URL list
    app.post(Consts.Paths.Product.IMPORT_URL_LIST, (ctx) -> {
      upload(ctx, "text/plain", genericImportService, ImportType.URL);
    }, AccessRoles.EDITOR());

    // upload ebay SKU list
    app.post(Consts.Paths.Product.IMPORT_EBAY_SKU, (ctx) -> {
      upload(ctx, "text/plain", genericImportService, ImportType.EBAY_SKU);
    }, AccessRoles.EDITOR());

    // upload amazon ASIN list
    app.post(Consts.Paths.Product.IMPORT_AMAZON_ASIN, (ctx) -> {
      upload(ctx, "text/plain", genericImportService, ImportType.AMAZON_ASIN);
    }, AccessRoles.EDITOR());

  }

  private void upload(Context ctx, String contentType, ImportService importService) {
    upload(ctx, contentType, importService, null);
  }

  private void upload(Context ctx, String contentType, ImportService importService, ImportType importType) {
    UploadedFile file = ctx.uploadedFile("file");
    if (file != null && file.getSize() >= 32) { // byte
      if (file.getContentType().equals(contentType)) {
        try {
          String content = IOUtils.toString(file.getContent(), StandardCharsets.UTF_8.name());
          Response result = (importType == null ? importService.upload(content)
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
