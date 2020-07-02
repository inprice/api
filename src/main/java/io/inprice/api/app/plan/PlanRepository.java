package io.inprice.api.app.plan;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyRepository;
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
    if (cacheMap == null) {
      synchronized(log) {
       if (cacheMap == null) {
         cacheMap = new HashMap<>();
         List<Plan> plans = db.findMultiple(con, "select * from plan ", this::map);
         if (plans != null && plans.size() > 0) {
           for (Plan plan : plans) {
             cacheMap.put(plan.getId(), plan);
           }
         }
       }
      }
    }
    return cacheMap.get(id);
  }

  public int findAllowedProductCount() {
    int result = 0;

    Plan model = db.findSingle("select p.* from plan as p inner " + 
      "join company as c on p.id = c.plan_id " +
      "where c.id = " + CurrentUser.getCompanyId(), this::map);
    if (model != null) {
      result = model.getProductLimit();
    }

    return result;
  }

  private Plan map(ResultSet rs) {
    try {
      Plan model = new Plan();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setName(rs.getString("name"));
      model.setDescription(rs.getString("description"));
      model.setPrice(rs.getBigDecimal("price"));
      model.setProductLimit(rs.getInt("product_limit"));
      model.setOrderNo(rs.getInt("order_no"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set plan's properties", e);
    }
    return null;
  }

}
