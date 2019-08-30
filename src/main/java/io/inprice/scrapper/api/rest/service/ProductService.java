package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.api.rest.validator.ProductDTOValidator;

public class ProductService {

    private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse getList() {
        return repository.getList();
    }

    public ServiceResponse insert(ProductDTO productDTO) {
        ServiceResponse res = ProductDTOValidator.validate(productDTO);
        if (res.isOK()) {
            res = repository.insert(productDTO);
        }
        return res;
    }

    public ServiceResponse update(ProductDTO productDTO) {
        if (productDTO.getId() == null || productDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        ServiceResponse res = ProductDTOValidator.validate(productDTO);
        if (res.isOK()) {
            res = repository.update(productDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return repository.deleteById(id);
    }

    public ServiceResponse toggleStatus(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return repository.toggleStatus(id);
    }

}
