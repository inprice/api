package io.inprice.api.validator;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.info.ProductDTO;

public class ProductValidator {
  
  public static String validate(ProductDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getCode())) {
      problem = "Product code cannot be null!";
    } else if (dto.getCode().length() < 3 || dto.getCode().length() > 50) {
      problem = "Product code must be between 3 and 50 chars!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getName())) {
        problem = "Product name cannot be null!";
      } else if (dto.getName().length() < 3 || dto.getName().length() > 500) {
        problem = "Product name must be between 3 and 500 chars!";
      }
    }

    if (problem == null) {
      if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ONE) < 0) {
        problem = "Price must be greater than zero!";
      }
    }

    if (problem == null) {
      dto.setCompanyId(CurrentUser.getCompanyId());
      dto.setCode(SqlHelper.clear(dto.getCode()));
      dto.setName(SqlHelper.clear(dto.getName()));
    }

    return problem;
  }

}
