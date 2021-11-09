package io.inprice.api.app.product;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.dto.ProductDTO;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.SqlHelper;

/**
 * Used by two classes; a) ProductService, b) ProductEximService
 * 
 * @author mdpinar
 * @since 2021-11-05
 */
public class ProductVerifier {

  public static String verify(ProductDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getSku())) {
      problem = "Sku cannot be empty!";
    } else if (dto.getSku().length() < 3 || dto.getSku().length() > 50) {
  		problem = "Sku must be between 3 - 50 chars!";
    }

    if (problem == null && StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be empty!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 250) {
      problem = "Name must be between 3 - 250 chars!";
    }

    if (problem == null) {
    	if (dto.getPrice() == null) {
    		problem = "Price cannot be empty!";
    	} else if (dto.getPrice().compareTo(BigDecimal.ZERO) < 1) {
    		problem = "Price must be greater than zero!";
    	} else if (dto.getPrice().compareTo(new BigDecimal(9_999_999)) > 0) {
    		problem = "Price is out of reasonable range!";
    	}
    }

    if (problem == null && dto.getBasePrice() != null) {
    	if (dto.getBasePrice().compareTo(BigDecimal.ZERO) < 1) {
	  		problem = "Base Price must be greater than zero!";
	  	} else if (dto.getBasePrice().compareTo(new BigDecimal(9_999_999)) > 0) {
	  		problem = "Base Price is out of reasonable range!";
	    }
    } else if (dto.getBasePrice() == null) {
    	dto.setBasePrice(BigDecimal.ZERO);
    }

    if (problem == null && dto.getBrand() != null && dto.getBrand().getId() == null && StringUtils.isNotBlank(dto.getBrand().getName())) {
    	if (dto.getBrand().getName().length() < 2 || dto.getBrand().getName().length() > 50) {
    		problem = "Brand name must be between 2 - 50 chars!";
    	}
    }

    if (problem == null && dto.getCategory() != null && dto.getCategory().getId() == null && StringUtils.isNotBlank(dto.getCategory().getName())) {
    	if (dto.getCategory().getName().length() < 2 || dto.getCategory().getName().length() > 50) {
    		problem = "Category name must be between 2 - 50 chars!";
    	}
    }

    if (problem == null) {
      dto.setWorkspaceId(CurrentUser.getWorkspaceId());
      dto.setSku(SqlHelper.clear(dto.getSku()));
      dto.setName(SqlHelper.clear(dto.getName()));
      if (dto.getPrice() == null) dto.setPrice(BigDecimal.ZERO);
      if (dto.getBrand() != null) dto.setBrandId(dto.getBrand().getId());
      if (dto.getCategory() != null) dto.setCategoryId(dto.getCategory().getId());
    }

    return problem;
  }

}
