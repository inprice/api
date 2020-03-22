package io.inprice.scrapper.api.app.product;

import java.util.Map;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.dto.ProductDTOValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;

public class ProductService {

   private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

   public ServiceResponse findById(Long id) {
      return repository.findById(id);
   }

   public ServiceResponse getList() {
      return repository.getList();
   }

   public ServiceResponse search(Map<String, String> searchMap) {
      SearchModel searchModel = new SearchModel(searchMap, Product.class);
      return repository.search(searchModel);
   }

   public ServiceResponse insert(ProductDTO dto) {
      if (dto != null) {
         ServiceResponse res = ProductDTOValidator.validate(dto);
         if (res.isOK()) {
            res = repository.insert(dto);
         }
         return res;
      }
      return Responses.Invalid.PRODUCT;
   }

   public ServiceResponse update(ProductDTO dto) {
      if (dto != null) {
         if (dto.getId() == null || dto.getId() < 1) {
            return Responses.NotFound.PRODUCT;
         }

         ServiceResponse res = ProductDTOValidator.validate(dto);
         if (res.isOK()) {
            res = repository.update(dto);
         }
         return res;
      }
      return Responses.Invalid.PRODUCT;
   }

   public ServiceResponse deleteById(Long id) {
      if (id == null || id < 1)
         return Responses.NotFound.PRODUCT;
      return repository.deleteById(id);
   }

   public ServiceResponse toggleStatus(Long id) {
      if (id == null || id < 1)
         return Responses.NotFound.PRODUCT;
      return repository.toggleStatus(id);
   }

}
