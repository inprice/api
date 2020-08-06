package io.inprice.api.app.plan;

import java.math.BigDecimal;
import java.util.HashMap;
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
        "Up to 5 products with unlimited competitors.",
        new BigDecimal(5),
        5,
        "price_1H8hpfBiHTcqawyMTooghKgi"
      ),
      new Plan(
        2, 
        "Micro Plus Plan",
        "Up to 10 products with unlimited competitors.",
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

  public ServiceResponse getPlans() {
    Map<String, Object> dataMap = new HashMap<>(2);
    dataMap.put("cid", CurrentUser.getCompanyId());
    dataMap.put("plans", plansMap.values());
    return new ServiceResponse(dataMap);
  }

}
