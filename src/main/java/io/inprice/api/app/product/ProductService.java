package io.inprice.api.app.product;

import com.rabbitmq.client.Channel;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductDTOValidator;
import io.inprice.api.dto.ProductSearchDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.ProductDTO;

public class ProductService {

  private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

  public ServiceResponse findById(Long id) {
    return repository.findById(id);
  }

  public ServiceResponse findEverythingById(Long id) {
    return repository.findEverythingById(id);
  }

  public ServiceResponse simpleSearch(String term) {
    return repository.simpleSearch(term);
  }

  public ServiceResponse fullSearch(ProductSearchDTO dto) {
    return repository.fullSearch(dto);
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
        if (res.isOK()) {
          Boolean hasPriceChanged = res.getData();
          if (hasPriceChanged.equals(Boolean.TRUE)) {
            Channel channel = RabbitMQ.openChannel();
            RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_PRICE_REFRESH_ROUTING(), dto.getId().toString());
            RabbitMQ.closeChannel(channel);
          }
        }
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
