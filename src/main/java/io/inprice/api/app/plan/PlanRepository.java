package io.inprice.api.app.plan;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stripe.model.Product;
import com.stripe.model.ProductCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyRepository;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Plan;

public class PlanRepository {

  private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private Map<Long, Plan> cacheMap;

  /**
   * Plans don't change frequently, so we can cache them
   *
   */
  public Plan findById(Connection con, Long id) {
    return getCacheMap(con).get(id);
  }

  public int findAllowedProductCount() {
    Plan found = getCacheMap().get(CurrentUser.getPlanId());
    if (found != null)
      return found.getProductLimit();
    return 0;
  }

  public ServiceResponse getList() {
    return new ServiceResponse(getCacheMap().values());
  }

  private Map<Long, Plan> getCacheMap() {
    return getCacheMap(null);
  }

  private Map<Long, Plan> getCacheMap(Connection con) {
    if (cacheMap == null) {
      synchronized (log) {
        if (cacheMap == null) {
          cacheMap = new HashMap<>();
          List<Plan> plans = null;
          if (con == null) {
            plans = db.findMultiple("select * from plan ", this::map);
          } else {
            plans = db.findMultiple(con, "select * from plan ", this::map);
          }
          if (plans != null && plans.size() > 0) {

            //fetching Stripe's product list to set all the plans' product id fields
            List<Product> stripeProdList = null;
            try {
              Map<String, Object> params = new HashMap<>();
              ProductCollection products = Product.list(params);
              stripeProdList = products.getData();
            } catch (Exception e) {
              log.error("Failed to fetch Stripe's product list", e);
            }

            for (Plan plan : plans) {
              if (stripeProdList != null && stripeProdList.size() > 0) {
                for (Product product : stripeProdList) {
                  if (product.getName().equals(plan.getName())) {
                    plan.setStripeProdId(product.getId());
                    break;
                  }
                }
              }
              cacheMap.put(plan.getId(), plan);
            }
          }
        }
      }
    }
    return cacheMap;
  }

  private Plan map(ResultSet rs) {
    try {
      Plan model = new Plan();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setActive(rs.getBoolean("active"));
      model.setOrderNo(rs.getInt("order_no"));
      model.setName(rs.getString("name"));
      model.setDescription(rs.getString("description"));
      model.setPrice(rs.getBigDecimal("price"));
      model.setProductLimit(rs.getInt("product_limit"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set plan's properties", e);
    }
    return null;
  }

}
