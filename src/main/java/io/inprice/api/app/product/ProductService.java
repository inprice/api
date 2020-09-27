package io.inprice.api.app.product;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductDTOValidator;
import io.inprice.api.dto.ProductSearchDTO;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Beans;
import io.inprice.common.info.ProductDTO;

public class ProductService {

  private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

  public Response findById(Long id) {
    return repository.findById(id);
  }

  public Response findEverythingById(Long id) {
    return repository.findEverythingById(id);
  }

  public Response simpleSearch(String term) {
    return repository.simpleSearch(term);
  }

  public Response fullSearch(ProductSearchDTO dto) {
    return repository.fullSearch(dto);
  }

  public Response insert(ProductDTO dto) {
    if (dto != null) {
      Response res = ProductDTOValidator.validate(dto);
      if (res.isOK()) {
        res = repository.insert(dto);
      }
      return res;
    }
    return Responses.Invalid.PRODUCT;
  }

  public Response update(ProductDTO dto) {
    if (dto != null) {
      if (dto.getId() == null || dto.getId() < 1) {
        return Responses.NotFound.PRODUCT;
      }

      Response res = ProductDTOValidator.validate(dto);
      if (res.isOK()) {
        res = repository.update(dto);
        //TODO: repository nin icinde yapılacak1
        //if (res.isOK()) {
        //  Boolean hasPriceChanged = res.getData();
        //  if (hasPriceChanged.equals(Boolean.TRUE)) {
        //    Channel channel = RabbitMQ.openChannel();
        //    RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_PRICE_REFRESH_ROUTING(), dto.getId().toString());
        //    RabbitMQ.closeChannel(channel);
        //  }
        //}
      }
      return res;
    }
    return Responses.Invalid.PRODUCT;
  }

  public Response deleteById(Long id) {
    if (id == null || id < 1)
      return Responses.NotFound.PRODUCT;
    return repository.deleteById(id);
  }

  public Response toggleStatus(Long id) {
    if (id == null || id < 1)
      return Responses.NotFound.PRODUCT;
    return repository.toggleStatus(id);
  }

}
