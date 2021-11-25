package io.inprice.api.app.product;

import java.util.Set;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.alarm.AlarmDao;
import io.inprice.api.app.smartprice.SmartPriceDao;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.converters.ProductRefreshResultConverter;
import io.inprice.common.formula.EvaluationResult;
import io.inprice.common.formula.FormulaHelper;
import io.inprice.common.helpers.AlarmHelper;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Product;
import io.inprice.common.models.SmartPrice;
import io.inprice.common.repository.CommonDao;

public class ProductPriceService {
	
	public static ProductRefreshResult refresh(Product oldProduct, Handle handle) {
		return refresh(Set.of(oldProduct), handle);
	}

	public static ProductRefreshResult refresh(Set<Product> oldProducts, Handle handle) {
		AlarmDao alarmDao = handle.attach(AlarmDao.class);
		SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
		CommonDao commonDao = handle.attach(CommonDao.class);
		
		ProductRefreshResult lastPRR = null;
		
		for (Product oldProduct: oldProducts) {
			lastPRR = ProductRefreshResultConverter.convert(commonDao.refreshProduct(oldProduct.getId()));

			//refreshing alarm indicators if needed
			if (oldProduct != null && lastPRR.getAlarmId() != null) {
				Alarm alarm = alarmDao.findById(oldProduct.getId(), CurrentUser.getWorkspaceId());
				if (alarm != null) {
					String query = AlarmHelper.generateAlarmUpdateQueryForProduct(oldProduct, lastPRR, alarm);
					if (query != null) {
			  		handle.execute(query);
					}
				}
			}

			//refreshing smart (aka suggested) price
			if (lastPRR.getSmartPriceId() != null) {
				SmartPrice smartPrice = smartPriceDao.findById(lastPRR.getSmartPriceId(), CurrentUser.getWorkspaceId());
      	if (smartPrice != null) {
	      	EvaluationResult result = FormulaHelper.evaluate(smartPrice, lastPRR);
	
		  		handle.execute(
		          "update product set suggested_price=?, suggested_price_problem=? where id=? ",
		          result.getValue(),
		          result.getProblem(),
		          oldProduct.getId()
	          );
      	}
			}
		}

		return lastPRR;
	}
	
}
