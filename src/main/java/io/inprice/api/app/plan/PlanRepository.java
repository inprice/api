package io.inprice.api.app.plan;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.models.Plan;

public class PlanRepository {

  private Map<Integer, Plan> plansMap;

  PlanRepository() {
    Plan[] plans = {
      new Plan(
        1, 
        "Micro Plan",
        "Up to 5 products with unlimeted competitors.",
        new BigDecimal(5),
        5,
        "price_1H8hpfBiHTcqawyMTooghKgi"
      ),
      new Plan(
        2, 
        "Micro Plus Plan",
        "Up to 10 products with unlimeted competitors.",
        new BigDecimal(7),
        10,
        "price_1H8hqXBiHTcqawyMvG5xVsdb"
      )
    };

    plansMap = new TreeMap<>();
    for (Plan plan : plans) {
      plansMap.put(plan.getId(), plan);
    }
  }

  public Plan findById(Integer id) {
    return plansMap.get(id);
  }

  public int findAllowedProductCount() {
    Plan found = findById(CurrentUser.getPlanId());
    if (found != null)
      return found.getProductLimit();
    return 0;
  }

  public ServiceResponse getList() {
    return new ServiceResponse(plansMap.values());
  }

}
