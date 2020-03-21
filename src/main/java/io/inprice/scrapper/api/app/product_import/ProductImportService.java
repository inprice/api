package io.inprice.scrapper.api.app.product_import;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class ProductImportService {

   private final ProductImportRepository repository = Beans.getSingleton(ProductImportRepository.class);

   public ServiceResponse findById(Long id) {
      return repository.findById(id);
   }

   public ServiceResponse getList() {
      return repository.getList();
   }

   public ServiceResponse deleteById(Long id) {
      if (id == null || id < 1) {
         return Responses.NotFound.IMPORT;
      }
      return repository.deleteById(id);
   }

}
