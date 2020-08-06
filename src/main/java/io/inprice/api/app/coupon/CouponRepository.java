package io.inprice.api.app.coupon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.plan.PlanRepository;
import io.inprice.api.app.subscription.SubscriptionRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.SubsSource;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.Plan;
import io.inprice.common.models.SubsTrans;

public class CouponRepository {

  private static final Logger log = LoggerFactory.getLogger(CouponRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);
  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);
  private static final SubscriptionRepository subsRepository = Beans.getSingleton(SubscriptionRepository.class);

  public ServiceResponse getCoupons() {
    List<Coupon> coupons = 
      db.findMultiple(
        String.format(
          "select * from coupon where issued_company_id=%d order by issued_at desc",
          CurrentUser.getCompanyId()
        ),
        this::map
      );
    if (coupons != null && coupons.size() > 0) {
      return new ServiceResponse(coupons);
    }
    return Responses.NotFound.COUPON;
  }

  public ServiceResponse applyCoupon(String code) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      Coupon coupon = db.findSingle(con, "select * from coupon where code='" + SqlHelper.clear(code) + "'", this::map);
      if (coupon != null) {
        if (coupon.getIssuedAt() == null) {
          if (coupon.getIssuedCompanyId() == null || coupon.getIssuedCompanyId().equals(CurrentUser.getCompanyId())) {
          
            Company company = 
              db.findSingle(con, 
                String.format("select * from company where id=%d", CurrentUser.getCompanyId()),
                this::mapForCompany
              );

            if (! company.getSubsStatus().equals(SubsStatus.ACTIVE)
            ||  ! company.getSubsStatus().equals(SubsStatus.COUPONED)) {

              final String query = 
                "update company " +
                "   set plan_id=?, subs_status=?, subs_renewal_at=DATE_ADD(now(), interval ? day), product_limit=? " +
                "where id=?";

              Integer planId = company.getPlanId();
              Integer productLimit = company.getProductLimit();

              boolean usePlanProdLimit = false;
              if (company.getPlanId() == null || coupon.getPlanId().compareTo(company.getPlanId()) > 0) {
                planId = coupon.getPlanId();
                usePlanProdLimit = true;
              }

              Plan plan = planRepository.findById(planId);
              if (usePlanProdLimit) {
                productLimit = plan.getProductLimit();
              }

              try (PreparedStatement pst = con.prepareStatement(query)) {
                int i = 0;
                pst.setLong(++i, planId);
                pst.setString(++i, SubsStatus.COUPONED.name());
                pst.setInt(++i, coupon.getDays());
                pst.setInt(++i, productLimit);
                pst.setLong(++i, CurrentUser.getCompanyId());

                if (pst.executeUpdate() > 0) {

                  try (PreparedStatement pstCoupon = con.prepareStatement("update coupon set issued_at=now() where code=?")) {
                    pstCoupon.setString(1, coupon.getCode());

                    if (pstCoupon.executeUpdate() > 0) {

                      SubsTrans trans = new SubsTrans();
                      trans.setCompanyId(CurrentUser.getCompanyId());
                      trans.setEventId(coupon.getCode());
                      trans.setEvent(SubsEvent.COUPON_USED);
                      trans.setSource(SubsSource.COUPON);
                      trans.setSuccessful(Boolean.TRUE);
                      trans.setReason("coupon");
                      trans.setDescription(coupon.getCode() + " is used.");
                      subsRepository.insertTrans(con, trans);
        
                      Map<String, Object> data = new HashMap<>(3);
                      data.put("planId", coupon.getPlanId());
                      data.put("subsStatus", SubsStatus.COUPONED);
                      data.put("subsRenewalAt", DateUtils.addDays(new Date(), coupon.getDays()));
                      res = new ServiceResponse(data);
                      log.info("Coupon {}, is issued for {}", coupon.getCode(), company.getName());
                    }
                  }
                }
              }
            } else {
              res = Responses.Already.ACTIVE_SUBSCRIPTION;
            }
          } else {
            res = Responses.Illegal.COUPON_ISSUED_FOR_ANOTHER_COMPANY;
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

  private Company mapForCompany(ResultSet rs) {
    try {
      Company model = new Company();
      model.setName(rs.getString("name"));
      model.setPlanId(RepositoryHelper.nullIntegerHandler(rs, "plan_id"));
      model.setProductLimit(rs.getInt("product_limit"));
      model.setSubsStatus(SubsStatus.valueOf(rs.getString("subs_status")));
      model.setSubsRenewalAt(rs.getTimestamp("subs_renewal_at"));

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
      model.setPlanId(RepositoryHelper.nullIntegerHandler(rs, "plan_id"));
      model.setIssuedCompanyId(RepositoryHelper.nullLongHandler(rs, "issued_company_id"));
      model.setIssuedAt(rs.getTimestamp("issued_at"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set coupon's properties", e);
    }
    return null;
  }

}
