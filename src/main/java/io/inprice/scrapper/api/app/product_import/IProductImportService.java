package io.inprice.scrapper.api.app.product_import;

/**
 * IProductImportService
 */
public interface IProductImportService {

   ImportProduct upload(String content);

   default ImportProduct upload(ImportType importType, String content) {
      return null;
   }

}