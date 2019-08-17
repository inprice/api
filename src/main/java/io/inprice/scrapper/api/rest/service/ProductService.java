package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

    public ServiceResponse findById(AuthUser authUser, Long id) {
        return repository.findById(authUser, id);
    }

    public ServiceResponse getList(AuthUser authUser) {
        return repository.getList(authUser);
    }

    public ServiceResponse insert(AuthUser authUser, ProductDTO productDTO) {
        ServiceResponse res = validate(productDTO, true);
        if (res.isOK()) {
            res = repository.insert(authUser, productDTO);
        }
        return res;
    }

    public ServiceResponse update(AuthUser authUser, ProductDTO productDTO) {
        if (productDTO.getId() == null || productDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        ServiceResponse res = validate(productDTO, false);
        if (res.isOK()) {
            res = repository.update(authUser, productDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(AuthUser authUser, Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return repository.deleteById(authUser, id);
    }

    public ServiceResponse toggleStatus(AuthUser authUser, Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return repository.toggleStatus(authUser, id);
    }

    private ServiceResponse validate(ProductDTO productDTO, boolean insert) {
        List<Problem> problems = new ArrayList<>();

        if (! StringUtils.isBlank(productDTO.getCode()) && productDTO.getCode().length() > 120) {
            problems.add(new Problem("code", "Product code can be up to 120 chars!"));
        }

        if (! StringUtils.isBlank(productDTO.getBrand()) && productDTO.getBrand().length() > 100) {
            problems.add(new Problem("brand", "Brand can be up to 100 chars!"));
        }

        if (! StringUtils.isBlank(productDTO.getCategory()) && productDTO.getCategory().length() > 100) {
            problems.add(new Problem("category", "Category can be up to 100 chars!"));
        }

        if (productDTO.getPrice() == null || productDTO.getPrice().compareTo(BigDecimal.ONE) < 0) {
            problems.add(new Problem("price", "Price must be greater than zero!"));
        }

        if (StringUtils.isBlank(productDTO.getName())) {
            problems.add(new Problem("name", "Product name cannot be null!"));
        } else if (productDTO.getName().length() < 3 || productDTO.getName().length() > 500) {
            problems.add(new Problem("name", "Product name must be between 3 and 500 chars!"));
        }

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
