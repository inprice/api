package io.inprice.api.app.smartprice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.formula.EvaluationResult;
import io.inprice.common.formula.FormulaHelper;
import io.inprice.common.formula.SmartPriceDTO;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.models.Product;
import io.inprice.common.models.SmartPrice;

public class SmartPriceService {

  Response search(String value) {
    try (Handle handle = Database.getHandle()) {
      SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      if (value == null) value = "";
      value = SqlHelper.clear(value) + "%";
      return new Response(smartPriceDao.search(value, CurrentUser.getWorkspaceId()));
    }
  }

  Response list() {
    try (Handle handle = Database.getHandle()) {
      SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      return new Response(smartPriceDao.list(CurrentUser.getWorkspaceId()));
    }
  }

  Response insert(SmartPriceDTO dto) {
  	Response res = Responses.DataProblem.DB_PROBLEM;
  	
    String problem = validate(dto);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);

        SmartPrice found = smartPriceDao.findByName(dto.getName(), CurrentUser.getWorkspaceId());
        if (found == null) {
        	Long id = smartPriceDao.insert(dto, CurrentUser.getWorkspaceId());
        	if (id != null && id > 0) {
        		res = Responses.OK;
          }
        } else {
        	res = Responses.Already.Defined.SMART_PRICE;
        }
      }
    } else {
    	res = new Response(problem);
    }

    return res;
  }

  Response update(SmartPriceDTO dto) {
  	Response res = Responses.NotFound.SMART_PRICE;

  	if (dto.getId() != null && dto.getId() > 0) {
      String problem = validate(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
          
          SmartPrice oldForm = smartPriceDao.findById(dto.getId(), CurrentUser.getWorkspaceId());
          if (oldForm != null) {
          	
	          //to prevent duplication, checking if any smartPrice other than this has the same name!
	          SmartPrice found = smartPriceDao.findByName(dto.getName(), dto.getId(), CurrentUser.getWorkspaceId());
	          if (found == null) {
            	
          		handle.begin();
	          	
	          	boolean isUpdated = smartPriceDao.update(dto, CurrentUser.getWorkspaceId());
	            if (isUpdated) {

	            	String oldFormulas = (oldForm.getFormula()+oldForm.getLowerLimitFormula()+oldForm.getUpperLimitFormula());
	            	String newFormulas = (dto.getFormula()+dto.getLowerLimitFormula()+dto.getUpperLimitFormula());

		          	//if there is a change on formulas, we need to update every related product
		          	if (oldFormulas.equals(newFormulas) == false) {
		          		List<Product> productList = smartPriceDao.findProductListBySmartPriceId(dto.getId(), CurrentUser.getWorkspaceId());
		          		if (CollectionUtils.isNotEmpty(productList)) {

		                Batch batch = handle.createBatch();
		          			
		          			for (Product product: productList) {
		          				ProductRefreshResult prr = new ProductRefreshResult();
		          				prr.setActives(product.getActives());
		          				prr.setProductPrice(product.getPrice());
		          				prr.setMinPrice(product.getMinPrice());
		          				prr.setAvgPrice(product.getAvgPrice());
		          				prr.setMaxPrice(product.getMaxPrice());

		          		  	EvaluationResult result = FormulaHelper.evaluate(product.getSmartPrice(), prr);
			                batch.add(
	                			String.format(
		          		        "update product set suggested_price=%f, suggested_price_problem=%s where id=%d ",
		          		        result.getValue(),
		          		        (result.getProblem() != null ? "'"+result.getProblem()+"'" : "null"),
		          		        product.getId()
	          		        )
	          		      );
		          			}
		          			batch.execute();
		          		}
		          	}
		          	handle.commit();
		          	res = Responses.OK;

	            } else {
		          	res = Responses.DataProblem.DB_PROBLEM;
	            }
	          } else {
	          	res = Responses.Already.Defined.SMART_PRICE;
	          }
          }
        }
      } else {
      	res = new Response(problem);
      }
  	}

  	return res;
  }

  Response delete(Long id) {
  	Response res = Responses.NotFound.SMART_PRICE;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      	SmartPrice smartPrice = smartPriceDao.findById(id, CurrentUser.getWorkspaceId());

      	if (smartPrice != null) {
        	handle.begin();
        	
        	smartPriceDao.releaseProducts(id, CurrentUser.getWorkspaceId());
        	boolean isOK = smartPriceDao.delete(id, CurrentUser.getWorkspaceId());

        	if (isOK) {
            handle.commit();
            res = Responses.OK;
          } else {
          	handle.rollback();
          	res = Responses.DataProblem.DB_PROBLEM;
      		}
      	}
      }
    }

    return res;
  }

  Response findById(Long id) {
  	Response res = Responses.NotFound.SMART_PRICE;
    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      	SmartPrice smartPrice = smartPriceDao.findById(id, CurrentUser.getWorkspaceId());
      	if (smartPrice != null) {
          res = new Response(smartPrice);
      	}
      }
    }
    return res;
  }

  Response test(SmartPriceDTO dto) {
  	Response res = Responses.DataProblem.DB_PROBLEM;
  	
    String problem = validate(dto);
    if (problem == null) {
  		SmartPrice smartPrice = new SmartPrice();
  		smartPrice.setFormula(dto.getFormula());
  		smartPrice.setLowerLimitFormula(dto.getLowerLimitFormula());
  		smartPrice.setUpperLimitFormula(dto.getUpperLimitFormula());

    	ProductRefreshResult prr = new ProductRefreshResult();
    	prr.setActives(3);
  		prr.setMinPrice(BigDecimal.valueOf(100));
  		prr.setAvgPrice(BigDecimal.valueOf(200));
  		prr.setMaxPrice(BigDecimal.valueOf(300));
  		
    	double[] prices = { 50.0, 100.0, 150.0, 200.0, 250.0, 300.0, 350.0 };
    	List<EvaluationResult> resultList = new ArrayList<>(prices.length);

    	for (double price: prices) {
    		prr.setProductPrice(BigDecimal.valueOf(price));
    		EvaluationResult er = FormulaHelper.evaluate(smartPrice, prr);
    		resultList.add(er);
    	}
    	res = new Response(resultList);
    } else {
    	res = new Response(problem);
    }

    return res;
  }
  
  private String validate(SmartPriceDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be empty!";
    } else if (dto.getName().length() > 70) {
      problem = "Name can be up to 70 chars!";
    }

    if (problem == null && StringUtils.isBlank(dto.getFormula())) {
      problem = "Formula cannot be empty!";
    } else if (dto.getFormula().length() > 255) {
      problem = "Formula can be up to 255 chars!";
    }

    if (problem == null && StringUtils.isNotBlank(dto.getLowerLimitFormula()) && dto.getLowerLimitFormula().length() > 255) {
  		problem = "Lower Limit Formula can be up to 255 chars!";
    }

    if (problem == null && StringUtils.isNotBlank(dto.getUpperLimitFormula()) && dto.getUpperLimitFormula().length() > 255) {
  		problem = "Upper Limit Formula can be up to 255 chars!";
    }

    if (problem == null) {
    	problem = FormulaHelper.verify(dto);
    }
    
    if (problem == null) {
    	dto.setName(SqlHelper.clear(dto.getName()));
    }
    
    return problem;
  }

}
