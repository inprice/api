package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.ProductImportRepository;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.api.rest.validator.ProductDTOValidator;

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
            return InstantResponses.NOT_FOUND("Import");
        }

        return repository.deleteById(id);
    }

}
