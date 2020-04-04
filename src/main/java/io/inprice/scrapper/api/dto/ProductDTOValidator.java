package io.inprice.scrapper.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.info.ServiceResponse;

public class ProductDTOValidator {

    public static ServiceResponse validate(ProductDTO dto) {
        List<String> problems = new ArrayList<>();

        if (StringUtils.isBlank(dto.getCode())) {
            problems.add("Product code cannot be null!");
        } else if (dto.getCode().length() < 3 || dto.getCode().length() > 120) {
            problems.add("Product code must be between 3 and 120 chars!");
        }

        if (StringUtils.isBlank(dto.getName())) {
            problems.add("Product name cannot be null!");
        } else if (dto.getName().length() < 3 || dto.getName().length() > 500) {
            problems.add("Product name must be between 3 and 500 chars!");
        }

        if (! StringUtils.isBlank(dto.getBrand()) && dto.getBrand().length() > 100) {
            problems.add("Brand can be up to 100 chars!");
        }

        if (! StringUtils.isBlank(dto.getCategory()) && dto.getCategory().length() > 100) {
            problems.add("Category can be up to 100 chars!");
        }

        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ONE) < 0) {
            problems.add("Price must be greater than zero!");
        }

        return Commons.createResponse(problems);
    }

}
