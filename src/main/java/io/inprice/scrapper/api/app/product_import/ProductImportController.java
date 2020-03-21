package io.inprice.scrapper.api.app.product_import;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.rest.component.Commons;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

public class ProductImportController {

   private static final ProductImportService importService = Beans.getSingleton(ProductImportService.class);
   private static final ProductCSVImportService csvImportService = Beans.getSingleton(ProductCSVImportService.class);
   private static final ProductURLImportService urlImportService = Beans.getSingleton(ProductURLImportService.class);
   private static final ProductCodeImportService codeImportService = Beans.getSingleton(ProductCodeImportService.class);

   @Routing
   public void addRoutes(Javalin app) {

      // find
      app.get(Consts.Paths.Product.IMPORT_BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, importService.findById(id)));
      });

      // list
      app.get(Consts.Paths.Product.IMPORT_BASE + "s", (ctx) -> {
         ctx.json(Commons.createResponse(ctx, importService.getList()));
      });

      // delete
      app.delete(Consts.Paths.Product.IMPORT_BASE + "/:id", (ctx) -> {
         Long id = ctx.pathParam("id", Long.class).check(it -> it > 0).getValue();
         ctx.json(Commons.createResponse(ctx, importService.deleteById(id)));
      });

      // upload csv
      app.post(Consts.Paths.Product.IMPORT_CSV, (ctx) -> {
         upload(ctx, "text/csv", csvImportService);
      });

      // upload URL list
      app.post(Consts.Paths.Product.IMPORT_URL_LIST, (ctx) -> {
         upload(ctx, "text/plain", urlImportService);
      });

      // upload ebay SKU list
      app.post(Consts.Paths.Product.IMPORT_EBAY_SKU_LIST, (ctx) -> {
         upload(ctx, "text/plain", codeImportService, ImportType.EBAY_SKU);
      });

      // upload amazon ASIN list
      app.post(Consts.Paths.Product.IMPORT_AMAZON_ASIN_LIST, (ctx) -> {
         upload(ctx, "text/plain", codeImportService, ImportType.AMAZON_ASIN);
      });

   }

   private void upload(Context ctx, String contentType, IProductImportService importService) {
      upload(ctx, contentType, importService, null);
   }

   private void upload(Context ctx, String contentType, IProductImportService importService, ImportType importType) {
      UploadedFile file = ctx.uploadedFile("file");
      if (file != null && file.getSize() >= 32) { // byte
         if (file.getContentType().equals(contentType)) {
            String content = IOUtils.toString(file.getContent(), StandardCharsets.UTF_8.name());
            ImportProduct result = (importType == null ? importService.upload(content)
                  : importService.upload(importType, content));
            ctx.json(createResponse(result));
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
