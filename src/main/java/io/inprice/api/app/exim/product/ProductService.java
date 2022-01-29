package io.inprice.api.app.exim.product;

import java.io.IOException;
import java.math.BigDecimal;
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

import io.inprice.api.app.exim.EximBase;
import io.inprice.api.app.exim.product.mapper.DownloadBean;
import io.inprice.api.app.exim.product.mapper.DownloadBeanMapper;
import io.inprice.api.app.product.ProductVerifier;
import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.models.Workspace;
import io.inprice.common.utils.StringHelper;
import io.javalin.http.Context;

public class ProductService extends EximBase {

  private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

  Response upload(String csvContent) {
  	Response res = Responses.OK;
  	List<String> problems = new ArrayList<>();

    try (Handle handle = Database.getHandle()) {
    	WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
    	Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());

    	if (workspace.getStatus().isActive()) {
    		int productLimit = workspace.getPlan().getProductLimit();
    		int productCount = workspace.getProductCount();

      	if (productCount < productLimit) {
		    	ProductDao dao = handle.attach(ProductDao.class);
		
		    	//to prevent multiple lookups to db
		    	Map<String, Long> brandMap = dao.getBrands(CurrentUser.getWorkspaceId());
		    	Map<String, Long> categoryMap = dao.getCategories(CurrentUser.getWorkspaceId());
		
		    	int lineNumber = 1;
		    	Set<String> looked = new HashSet<>(); //by sku
		    	List<ProductDTO> dtos = new ArrayList<>();
		    	
		    	handle.begin();
		    	
		    	String[] rows = csvContent.lines().toArray(String[]::new);
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
		      	if (lineNumber == 250) break;
		      	if (problems.size() >= 25) {
		      		problems.add("Cancelled because there are too many problems in your file!");
		      		dtos.clear();
		      		break;
		      	}
		      	if (productCount+lineNumber >= productLimit) break;
		      	lineNumber++;
		      }
		
		      //inserting the records
		      if (dtos.size() > 0) {
		      	dao.insertAll(dtos);
		      	workspaceDao.incProductCount(CurrentUser.getWorkspaceId(), dtos.size());
		      	handle.commit();
		      } else {
		      	handle.rollback();
		      }
		
		      Map<String, Object> result = Map.of(
		    		"total", rows.length,
		      	"successCount", dtos.size(),
		      	"problems", problems
		  		);
		      
		      res = new Response(result);
	    	} else {
	    		res = Responses.NotAllowed.NO_PRODUCT_LIMIT;
	    	}
    	} else {
    		res = Responses.NotAllowed.HAVE_NO_ACTIVE_PLAN;
    	}
		}
    
    return res;
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
  		dto.setSku(columns.get(0));
  		dto.setName(columns.get(1));
  		try {
	  		dto.setPrice(new BigDecimal(columns.get(2).trim()));
  		} catch (Exception e) { 
  			return Responses.Invalid.PRICE;
  		}
  
  		//tries to find brandId via brandMap. if fails then tries to insert db, and adds new brand into brandMap for later controls
  		if (columns.size() >= 4 && StringUtils.isNotBlank(columns.get(3))) {
				String brandName = SqlHelper.clear(columns.get(3));
  			Long brandId = brandMap.get(brandName);
  			if (brandId == null) {
					brandId = dao.insertBrand(brandName, CurrentUser.getWorkspaceId());
					brandMap.put(brandName, brandId);
				}
  			dto.setBrandId(brandId);
  		}

  		//tries to find categoryId via categoryMap. if fails then tries to insert db, and adds new category into categoryMap for later controls
  		if (columns.size() == 5 && StringUtils.isNotBlank(columns.get(4))) {
				String categoryName = SqlHelper.clear(columns.get(4));
				Long categoryId = categoryMap.get(categoryName);
				if (categoryId == null) {
					categoryId = dao.insertCategory(categoryName, CurrentUser.getWorkspaceId());
					categoryMap.put(categoryName, categoryId);
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

  Response download(Context ctx) {
    try (Handle handle = Database.getHandle()) {
      //---------------------------------------------------
      //fetching the data
      //---------------------------------------------------
    	handle.registerRowMapper(new DownloadBeanMapper());
      List<DownloadBean> dDeans =
        handle.createQuery(
          "select sku, p.name, price, brn.name as brand_name, cat.name as category_name from product as p " +
      		"left join brand as brn on brn.id = p.brand_id " +
      		"left join category as cat on cat.id = p.category_id " +
      		generateWhereClause(ctx) +
      		" order by sku"
        )
      .mapTo(DownloadBean.class)
      .list();

    	if (dDeans.size() > 0) {
    		StringBuilder lines = new StringBuilder();
    		for (DownloadBean bean: dDeans) {
    			lines.append(normalizeValue(bean.getSku()));
    			lines.append(normalizeValue(bean.getName()));
    			lines.append(bean.getPrice() + ",");
    			lines.append(normalizeValue(bean.getBrandName()));
    			lines.append(normalizeValue(bean.getCategoryName(), true));
    		}
    		IOUtils.copy(IOUtils.toInputStream(lines.toString(), "UTF-8") , ctx.res.getOutputStream());
    		return Responses.OK;
    	} else {
    		return Responses.NotFound.PRODUCT;
    	}
    } catch (IOException e) {
			logger.error("Failed to export categories", e);
		}

    return Responses.ServerProblem.EXCEPTION;
  }

	String generateWhereClause(Context ctx) {
		StringBuilder sql = new StringBuilder();

    sql.append("where p.workspace_id = ");
    sql.append(CurrentUser.getWorkspaceId());

  	if (ctx.queryParam("brand") != null) {
  		sql.append(" and brn.name = '");
  		sql.append(SqlHelper.clear(ctx.queryParam("brand")));
  		sql.append("'");
  	}

  	if (ctx.queryParam("category") != null) {
  		sql.append(" and cat.name = '");
			sql.append(SqlHelper.clear(ctx.queryParam("category")));
  		sql.append("'");
  	}
  	
  	if (ctx.queryParam("positions") != null) {
    	sql.append(
  			String.format(" and p.position in (%s) ", StringHelper.join("'", ctx.queryParams("positions")))
			);
    }

    return sql.toString();
	}

}
