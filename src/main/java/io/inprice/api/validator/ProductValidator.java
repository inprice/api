package io.inprice.api.validator;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.app.product.dto.ProductDTO;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.session.CurrentUser;

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

      if (dto.getTagsChanged() == null) dto.setTagsChanged(false);
      if (dto.getTagsChanged() &&  dto.getTags() != null && dto.getTags().size() > 0) {
        Set<String> newTags = new HashSet<>(dto.getTags().size());
        for (String tag: dto.getTags()) {
          if (StringUtils.isNotBlank(tag)) {
            if (tag.trim().length() < 30) {
              newTags.add(SqlHelper.clear(tag));
            } else {
              newTags.add(SqlHelper.clear(tag).substring(0, 30));
            }
          }
        }
        dto.setTags(newTags);
      }
    }

    return problem;
  }

}
