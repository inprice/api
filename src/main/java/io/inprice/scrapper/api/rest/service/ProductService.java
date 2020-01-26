package io.inprice.scrapper.api.rest.service;

import java.util.Map;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.api.rest.validator.ProductDTOValidator;
import io.inprice.scrapper.common.models.Product;

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

	public ServiceResponse insert(ProductDTO productDTO) {
		if (productDTO != null) {
			ServiceResponse res = ProductDTOValidator.validate(productDTO);
			if (res.isOK()) {
				res = repository.insert(productDTO);
			}
			return res;
		}
		return Responses.Invalid.PRODUCT;
	}

	public ServiceResponse update(ProductDTO productDTO) {
		if (productDTO != null) {
			if (productDTO.getId() == null || productDTO.getId() < 1) {
				return Responses.NotFound.PRODUCT;
			}

			ServiceResponse res = ProductDTOValidator.validate(productDTO);
			if (res.isOK()) {
				res = repository.update(productDTO);
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
