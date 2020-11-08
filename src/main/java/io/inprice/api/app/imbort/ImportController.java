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
  private static final URLImportService urlImportService = Beans.getSingleton(URLImportService.class);

  @Override
  public void addRoutes(Javalin app) {

    // find by id
    app.get(Consts.Paths.Product.IMPORT + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, urlImportService.findById(id)));
    }, AccessRoles.ANYONE());

    // delete
    app.delete(Consts.Paths.Product.IMPORT + "/:id", (ctx) -> {
      Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
      ctx.json(Commons.createResponse(ctx, urlImportService.deleteById(id)));
    }, AccessRoles.EDITOR());

    // get import list
    app.get(Consts.Paths.Product.IMPORT + "s", (ctx) -> {
      ctx.json(Commons.createResponse(ctx, urlImportService.getList()));
    }, AccessRoles.ANYONE());

    // upload CSV file
    app.post(Consts.Paths.Product.IMPORT_CSV_FILE, (ctx) -> {
      upload(ctx, "text/csv", csvImportService);
    }, AccessRoles.EDITOR());

    // upload URL file
    app.post(Consts.Paths.Product.IMPORT_URL_FILE, (ctx) -> {
      upload(ctx, "text/plain", urlImportService, ImportType.URL);
    }, AccessRoles.EDITOR());

    // upload ebay SKU file
    app.post(Consts.Paths.Product.IMPORT_EBAY_FILE, (ctx) -> {
      upload(ctx, "text/plain", urlImportService, ImportType.EBAY);
    }, AccessRoles.EDITOR());

    // upload amazon ASIN file
    app.post(Consts.Paths.Product.IMPORT_AMAZON_FILE, (ctx) -> {
      upload(ctx, "text/plain", urlImportService, ImportType.AMAZON);
    }, AccessRoles.EDITOR());

    // upload CSV list
    app.post(Consts.Paths.Product.IMPORT_CSV_LIST, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, csvImportService.upload(ctx.body())));
    }, AccessRoles.EDITOR());

    // upload URL list
    app.post(Consts.Paths.Product.IMPORT_URL_LIST, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, urlImportService.upload(ImportType.URL, ctx.body())));
    }, AccessRoles.EDITOR());

    // upload ebay SKU list
    app.post(Consts.Paths.Product.IMPORT_EBAY_LIST, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, urlImportService.upload(ImportType.EBAY, ctx.body())));
    }, AccessRoles.EDITOR());

    // upload amazon ASIN list
    app.post(Consts.Paths.Product.IMPORT_AMAZON_LIST, (ctx) -> {
      ctx.json(Commons.createResponse(ctx, urlImportService.upload(ImportType.AMAZON, ctx.body())));
    }, AccessRoles.EDITOR());

  }

  private void upload(Context ctx, String contentType, BaseImportService importService) {
    upload(ctx, contentType, importService, null);
  }

  private void upload(Context ctx, String contentType, BaseImportService importService, ImportType importType) {
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
