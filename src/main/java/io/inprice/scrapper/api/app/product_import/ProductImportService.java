package io.inprice.scrapper.api.app.product_import;

import io.inprice.scrapper.api.framework.Beans;

import java.util.Arrays;
import java.util.Map;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;

public class ProductImportService {

  private final ProductImportRepository repository = Beans.getSingleton(ProductImportRepository.class);

  public ServiceResponse findById(Long id) {
    return repository.findById(id);
  }

  public ServiceResponse search(Map<String, String> searchMap) {
    SearchModel sm = new SearchModel(searchMap, "created_at desc, status", ImportProduct.class);
    sm.setExactSearch(true);
    sm.setTable("import_product");
    sm.setFields(Arrays.asList("status"));
    return repository.search(sm);
  }

  public ServiceResponse deleteById(Long id) {
    if (id == null || id < 1) {
      return Responses.NotFound.IMPORT;
    }
    return repository.deleteById(id);
  }

}
