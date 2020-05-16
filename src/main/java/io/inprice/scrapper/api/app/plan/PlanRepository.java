package io.inprice.scrapper.api.app.plan;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.company.CompanyRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class PlanRepository {

   private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);
   private static final Database db = Beans.getSingleton(Database.class);

   public int findAllowedProductCount() {
      int result = 0;

      ServiceResponse res = findByCompanyId();
      if (res.isOK()) {
         Plan plan = res.getData();
         result = plan.getRowLimit();
      }

      return result;
   }

   private ServiceResponse findByCompanyId() {
      Plan model = db.findSingle("select p.* from plan as p inner join company as c on p.name = c.plan_name "
            + "where c.id = " + CurrentUser.getCompanyId(), PlanRepository::map);
      if (model != null) {
         return new ServiceResponse(model);
      }

      return Responses.Invalid.PLAN;
   }

   private static Plan map(ResultSet rs) {
      try {
         Plan model = new Plan();
         model.setName(rs.getString("name"));
         model.setDescription(rs.getString("description"));
         model.setPrice(rs.getBigDecimal("price"));
         model.setRowLimit(rs.getInt("row_limit"));
         model.setUserLimit(rs.getInt("user_limit"));
         model.setOrderNo(rs.getInt("order_no"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set plan's properties", e);
      }
      return null;
   }

}
