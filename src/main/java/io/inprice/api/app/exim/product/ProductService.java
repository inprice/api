package io.inprice.api.app.exim.product;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.product.ProductVerifier;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.utils.NumberHelper;
import io.inprice.common.utils.StringHelper;

public class ProductService {

  private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

  Response upload(String fileContent) {
  	List<String> problems = new ArrayList<>();

    try (Handle handle = Database.getHandle()) {

    	ProductDao dao = handle.attach(ProductDao.class);

    	//to prevent multiple lookups to db
    	Map<String, Long> brandMap = dao.getBrands(CurrentUser.getWorkspaceId());
    	Map<String, Long> categoryMap = dao.getCategories(CurrentUser.getWorkspaceId());

    	int lineNumber = 1;
    	Set<String> looked = new HashSet<>(); //by sku
    	List<ProductDTO> dtos = new ArrayList<>();
    	
    	String[] rows = fileContent.lines().toArray(String[]::new);
      for (String row : rows) {
      	if (StringUtils.isBlank(row)) continue;
      	
      	Response buildRes = buildProductDTO(row, dao, brandMap, categoryMap);
      	if (buildRes.isOK()) {
      		ProductDTO dto = buildRes.getData();

      		if (looked.contains(dto.getSku().toLowerCase())) {
	      		problems.add(lineNumber + ". " + dto.getSku()  + " duplicate!");
	    		} else {
		      	looked.add(dto.getSku().toLowerCase()); //for later controls to check if it is already handled!
	
	        	boolean exist = dao.doesSkuExist(dto.getSku(), CurrentUser.getWorkspaceId());
	        	if (exist) {
	        		problems.add(lineNumber + ". " + dto.getSku() + " already exists!");
	        	} else {
	        		dtos.add(dto);
	        	}
	        }
      	} else {
      		problems.add(lineNumber + ". " + buildRes.getReason());
      	}
      	if (lineNumber == 1000) break;
      	if (problems.size() >= 25) {
      		problems.add("Cancelled because there are too many problems in your file!");
      		dtos.clear();
      		break;
      	}
      	lineNumber++;
      }

      //inserting the records
      if (dtos.size() > 0) {
      	handle.begin();
      	dao.insertAll(dtos);
      	handle.commit();
      }

      Map<String, Object> result = Map.of(
    		"total", rows.length,
      	"successCount", dtos.size(),
      	"problems", problems
  		);
      
      return new Response(result);
		}
  }

  /**
   * Extract ProductDTO out of csv line
   * 
   * @param line - csv line
   * @param brandMap - brand lookup map to check if the same has already been defined previously 
   * @param categoryMap - category lookup map to check if the same has already been defined previously
   * 
   * @return ProductDTO
   */
  private Response buildProductDTO(String line, ProductDao dao, Map<String, Long> brandMap, Map<String, Long> categoryMap) {
  	ProductDTO dto = new ProductDTO();

  	//the first three columns sku, name and price are mandatory
  	//the next two columns brand and category names are optional 
  	List<String> columns = StringHelper.splitCSV(line);
  	if (columns.size() >= 3 && columns.size() <= 5) {
  		dto.setWorkspaceId(CurrentUser.getWorkspaceId());
  		dto.setSku(SqlHelper.clear(columns.get(0)));
  		dto.setName(SqlHelper.clear(columns.get(1)));
  		try {
	  		BigDecimal price = new BigDecimal(NumberHelper.extractPrice(columns.get(2)));
	  		dto.setPrice(price.setScale(2, RoundingMode.HALF_UP));
  		} catch (Exception e) { 
  			return Responses.Invalid.PRICE;
  		}
  
  		//tries to find brandId via brandMap. if fails then tries to insert db, and adds new brand into brandMap for later controls
  		if (columns.size() >= 4 && StringUtils.isNotBlank(columns.get(3))) {
				String brandName = SqlHelper.clear(columns.get(3));
  			Long brandId = brandMap.get(brandName.toLowerCase());
  			if (brandId == null) {
					brandId = dao.insertBrand(brandName, CurrentUser.getWorkspaceId());
					brandMap.put(brandName.toLowerCase(), brandId);
				}
  			dto.setBrandId(brandId);
  		}

  		//tries to find categoryId via categoryMap. if fails then tries to insert db, and adds new category into categoryMap for later controls
  		if (columns.size() == 5 && StringUtils.isNotBlank(columns.get(4))) {
				String categoryName = SqlHelper.clear(columns.get(4));
				Long categoryId = categoryMap.get(categoryName.toLowerCase());
				if (categoryId == null) {
					categoryId = dao.insertCategory(categoryName, CurrentUser.getWorkspaceId());
					categoryMap.put(categoryName.toLowerCase(), categoryId);
				}
				dto.setCategoryId(categoryId);
  		}

  		String problem = ProductVerifier.verify(dto);
  		if (problem != null) {
  			return new Response(problem);
  		} else {
  			return new Response(dto);
  		}
		} else {
			return Responses.Invalid.CSV_COLUMN_COUNT;
  	}
  }

  Response download(OutputStream outputStream) {
    try (Handle handle = Database.getHandle()) {
    	ProductDao dao = handle.attach(ProductDao.class);
    	List<String[]> products = dao.getList(CurrentUser.getWorkspaceId());
    	if (products.size() > 0) {
    		StringBuilder lines = new StringBuilder("name\n");
    		for (String[] fields: products) {
    			lines.append(normalizeValue(fields[0], false));
    			lines.append(normalizeValue(fields[1], false));
    			lines.append(normalizeValue(fields[2], false));
    			lines.append(normalizeValue(fields[3], false));
    			lines.append(normalizeValue(fields[4], true));
    		}
    		IOUtils.copy(IOUtils.toInputStream(lines.toString(), "UTF-8") , outputStream);
    		return Responses.OK;
    	} else {
    		return Responses.NotFound.CATEGORY;
    	}
    } catch (IOException e) {
			logger.error("Failed to export categories", e);
		}

    return Responses.ServerProblem.EXCEPTION;
  }
  
  private String normalizeValue(String value, boolean isLastValue) {
  	StringBuilder sb = new StringBuilder();
  	if (StringUtils.isNotBlank(value)) {
			if (value.indexOf(',') >= 0) sb.append('"');
			sb.append(value);
			if (value.indexOf(',') >= 0) sb.append('"');
  	} else {
  		sb.append("");
  	}

  	if (isLastValue) {
			sb.append('\n');
		} else {
			sb.append(',');
		}
  	return sb.toString(); 
  }

}
