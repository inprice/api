package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductDTOValidator {

    public static ServiceResponse validate(ProductDTO productDTO) {
        List<Problem> problems = new ArrayList<>();

        if (StringUtils.isBlank(productDTO.getCode())) {
            problems.add(new Problem("code", "Product code cannot be null!"));
        } else if (productDTO.getCode().length() < 3 || productDTO.getCode().length() > 120) {
            problems.add(new Problem("code", "Product code must be between 3 and 120 chars!"));
        }

        if (StringUtils.isBlank(productDTO.getName())) {
            problems.add(new Problem("name", "Product name cannot be null!"));
        } else if (productDTO.getName().length() < 3 || productDTO.getName().length() > 500) {
            problems.add(new Problem("name", "Product name must be between 3 and 500 chars!"));
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

        return Commons.createResponse(problems);
    }

}
