package io.inprice.scrapper.api.app.coupon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.plan.PlanRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.session.info.ForRedis;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.PlanStatus;
import io.inprice.scrapper.common.models.Company;
import io.inprice.scrapper.common.models.Coupon;
import io.inprice.scrapper.common.models.Plan;

public class CouponRepository {

  private static final Logger log = LoggerFactory.getLogger(CouponRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);
  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

  public ServiceResponse applyCoupon(String code) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      Coupon coupon = db.findSingle(con, "select * from coupon where code='" + code + "'", this::map);
      if (coupon != null) {
        if (coupon.getIssuedCompanyId() == null) {
        
          Company company = db.findSingle(con, "select plan_id, product_limit, name, due_date from company where id=" + CurrentUser.getCompanyId(), this::mapForCompany);

          String query = "update company set plan_id=?, plan_status=?, due_date=DATE_ADD(now(), interval ? day), product_limit=? where id=?";
          if (company.getDueDate() != null && company.getDueDate().after(new Date())) {
            query = query.replace("now()", "due_date");
          }

          Long planId = company.getPlanId();
          Integer productLimit = company.getProductLimit();

          boolean usePlanProdLimit = false;
          if (company.getPlanId() == null || coupon.getPlanId().compareTo(company.getPlanId()) > 0) {
            planId = coupon.getPlanId();
            usePlanProdLimit = true;
          }

          Plan plan = planRepository.findById(con, planId);
          if (usePlanProdLimit) {
            productLimit = plan.getProductLimit();
          }

          try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setLong(++i, planId);
            pst.setString(++i, PlanStatus.ACTIVE.name());
            pst.setInt(++i, coupon.getDays());
            pst.setInt(++i, productLimit);
            pst.setLong(++i, CurrentUser.getUserId());

            if (pst.executeUpdate() > 0) {

              try (PreparedStatement pstCoupon = con.prepareStatement("update coupon set issued_company_id=?, issued_at=now() where code=?")) {
                pstCoupon.setLong(1, CurrentUser.getCompanyId());
                pstCoupon.setString(2, coupon.getCode());

                if (pstCoupon.executeUpdate() > 0) {
                  res = updateRedisSessionsForPlan(con, planId);
                  log.info("Coupon {}, is issued for {}", coupon.getCode(), company.getName());
                }
              }
            }
          }
        } else {
          res = Responses.Already.USED_COUPON;
        }
      } else {
        res = Responses.Invalid.COUPON;
      }

      if (res.isOK()) {
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to apply a coupon. Code: " + code, e);
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

  private ServiceResponse updateRedisSessionsForPlan(Connection con, Long planId) {
    List<String> hashes = db.findMultiple(con,
        String.format("select _hash from user_session where company_id=%d", CurrentUser.getCompanyId()), this::mapForHashField);

    if (hashes != null && hashes.size() > 0) {
      Map<String, ForRedis> map = new HashMap<>(hashes.size());
      for (String hash : hashes) {
        ForRedis ses = RedisClient.getSession(hash);
        ses.setPlanId(planId);
        map.put(hash, ses);
      }
      RedisClient.updateSessions(map);

      Map<String, Long> dataMap = new HashMap<>(1);
      dataMap.put("planId", planId);
      return new ServiceResponse(dataMap);
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  public ServiceResponse getCoupons() {
    List<Coupon> coupons = 
      db.findMultiple(
        "select * from coupon " + 
        "where issued_company_id=" + CurrentUser.getCompanyId() +
        " order by issued_at desc", 
        this::map);
    if (coupons != null && coupons.size() > 0) {
      return new ServiceResponse(coupons);
    }
    return Responses.NotFound.COUPON;
  }

  private String mapForHashField(ResultSet rs) {
    try {
      return rs.getString("_hash");
    } catch (SQLException e) {
      log.error("Failed to get _hash field from user_session table", e);
    }
    return null;
  }

  private Company mapForCompany(ResultSet rs) {
    try {
      Company model = new Company();
      model.setName(rs.getString("name"));
      model.setPlanId(RepositoryHelper.nullLongHandler(rs, "plan_id"));
      model.setProductLimit(rs.getInt("product_limit"));
      model.setDueDate(rs.getTimestamp("due_date"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set company's properties for coupon", e);
    }
    return null;
  }

  private Coupon map(ResultSet rs) {
    try {
      Coupon model = new Coupon();
      model.setCode(rs.getString("code"));
      model.setDescription(rs.getString("description"));
      model.setDays(rs.getInt("days"));
      model.setPlanId(RepositoryHelper.nullLongHandler(rs, "plan_id"));
      model.setIssuedAt(rs.getTimestamp("issued_at"));
      model.setIssuedCompanyId(RepositoryHelper.nullLongHandler(rs, "issued_company_id"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set coupon's properties", e);
    }
    return null;
  }

}
