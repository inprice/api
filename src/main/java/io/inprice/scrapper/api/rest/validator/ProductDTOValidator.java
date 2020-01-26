package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductDTOValidator {

    public static ServiceResponse validate(ProductDTO productDTO) {
        List<String> problems = new ArrayList<>();

        if (StringUtils.isBlank(productDTO.getCode())) {
            problems.add("Product code cannot be null!");
        } else if (productDTO.getCode().length() < 3 || productDTO.getCode().length() > 120) {
            problems.add("Product code must be between 3 and 120 chars!");
        }

        if (StringUtils.isBlank(productDTO.getName())) {
            problems.add("Product name cannot be null!");
        } else if (productDTO.getName().length() < 3 || productDTO.getName().length() > 500) {
            problems.add("Product name must be between 3 and 500 chars!");
        }

        if (! StringUtils.isBlank(productDTO.getBrand()) && productDTO.getBrand().length() > 100) {
            problems.add("Brand can be up to 100 chars!");
        }

        if (! StringUtils.isBlank(productDTO.getCategory()) && productDTO.getCategory().length() > 100) {
            problems.add("Category can be up to 100 chars!");
        }

        if (productDTO.getPrice() == null || productDTO.getPrice().compareTo(BigDecimal.ONE) < 0) {
            problems.add("Price must be greater than zero!");
        }

        return Commons.createResponse(problems);
    }

}
