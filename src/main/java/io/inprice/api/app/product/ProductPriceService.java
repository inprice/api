package io.inprice.api.app.product;

/**
 * In order to organize alarms and smart price, handles the operations regarding changing price of a product
 * 
 * @author mdpinar
 */
import java.math.BigDecimal;
import java.util.Set;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.alarm.AlarmDao;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.converters.ProductRefreshResultConverter;
import io.inprice.common.formula.EvaluationResult;
import io.inprice.common.formula.FormulaHelper;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Product;
import io.inprice.common.repository.CommonDao;

public class ProductPriceService {
	
	public static ProductRefreshResult refresh(Long productId, Handle handle) {
		return refresh(Set.of(productId), handle);
	}

	public static ProductRefreshResult refresh(Set<Long> productIdSet, Handle handle) {
		ProductDao productDao = handle.attach(ProductDao.class);
		AlarmDao alarmDao = handle.attach(AlarmDao.class);
		CommonDao commonDao = handle.attach(CommonDao.class);
		
		ProductRefreshResult lastPRR = null;
		
		for (Long pid: productIdSet) {
			lastPRR = ProductRefreshResultConverter.convert(commonDao.refreshProduct(pid));

			//refreshing alarm indicators if needed
			if (lastPRR.getAlarmId() != null) {
				Alarm alarm = alarmDao.findById(pid, CurrentUser.getWorkspaceId());
				if (alarm != null) {
					
			  	BigDecimal newAmount = BigDecimal.ZERO;
		  		switch (alarm.getSubject()) {
  	  			case MINIMUM: {
  	  				newAmount = lastPRR.getMinPrice();
  	  				break;
  	  			}
  	  			case AVERAGE: {
  	  				newAmount = lastPRR.getAvgPrice();
  	  				break;
  	  			}
  	  			case MAXIMUM: {
  	  				newAmount = lastPRR.getMaxPrice();
  	  				break;
  	  			}
  					default: break;
  				}
		  		
		  		handle.execute(
	  				"update alarm set last_position=?, last_amount=?, updated_at=now() where id=? ",
	          lastPRR.getPosition(),
	          newAmount,
	          lastPRR.getAlarmId()
					);
				}
			}

			//refreshing smart (aka suggested) price
			if (lastPRR.getSmartPriceId() != null) {
      	Product product = productDao.findByIdWithSmarPrice(pid, CurrentUser.getWorkspaceId());
      	if (product != null) {
	      	EvaluationResult result = FormulaHelper.evaluate(product.getSmartPrice(), lastPRR);
	
		  		handle.execute(
		          "update product set suggested_price=?, suggested_price_problem=? where id=? ",
		          result.getValue(),
		          result.getProblem(),
		          product.getId()
	          );
      	}
			}
		}

		return lastPRR;
	}
	
}
