package io.inprice.api.app.coupon;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.system.PlanDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.SubsSource;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.Plan;
import io.inprice.common.models.SubsTrans;
import io.inprice.common.utils.CouponManager;

class CouponService {

  private static final Logger log = LoggerFactory.getLogger(CouponService.class);

  Response getCoupons() {
    try (Handle handle = Database.getHandle()) {
      CouponDao couponDao = handle.attach(CouponDao.class);

      List<Coupon> coupons = couponDao.findListByIssuedCompanyId(CurrentUser.getCompanyId());
      if (coupons != null && coupons.size() > 0) {
        return new Response(coupons);
      }
      return Responses.NotFound.COUPON;
    }
  }

  Response applyCoupon(String code) {
    Response[] res = { Responses.Invalid.COUPON };

    if (CouponManager.isValid(code)) {

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(h -> {
          CouponDao couponDao = handle.attach(CouponDao.class);

          Coupon coupon = couponDao.findByCode(code);
          if (coupon != null) {
            if (coupon.getIssuedAt() == null) {
              if (coupon.getIssuedCompanyId() == null || coupon.getIssuedCompanyId().equals(CurrentUser.getCompanyId())) {

                CompanyDao companyDao = handle.attach(CompanyDao.class);

                Company company = companyDao.findById(CurrentUser.getCompanyId());
                if (!company.getSubsStatus().equals(SubsStatus.ACTIVE) || !company.getSubsStatus().equals(SubsStatus.COUPONED)) {

                  Integer planId = company.getPlanId();
                  Integer productLimit = company.getProductLimit();

                  boolean usePlanProdLimit = false;
                  if (company.getPlanId() == null || coupon.getPlanId().compareTo(company.getPlanId()) > 0) {
                    planId = coupon.getPlanId();
                    usePlanProdLimit = true;
                  }

                  Plan plan = PlanDao.getById(planId);
                  if (usePlanProdLimit) productLimit = plan.getProductLimit();

                  boolean isOK = 
                    companyDao.updateSubscription(
                      CurrentUser.getCompanyId(),
                      SubsStatus.COUPONED.name(),
                      coupon.getDays(),
                      productLimit, 
                      planId
                    );

                  if (isOK) {
                    isOK = couponDao.updateByCode(coupon.getCode());

                    if (isOK) {
                      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

                      SubsTrans trans = new SubsTrans();
                      trans.setCompanyId(CurrentUser.getCompanyId());
                      trans.setEventId(coupon.getCode());
                      trans.setEvent(SubsEvent.COUPON_USED);
                      trans.setSource(SubsSource.COUPON);
                      trans.setSuccessful(Boolean.TRUE);
                      trans.setReason("coupon");
                      trans.setDescription(coupon.getCode() + " is used.");

                      isOK = subscriptionDao.insertTrans(trans, SubsSource.COUPON.name(), SubsEvent.COUPON_USED.name());
                      
                      if (isOK) {
                        Map<String, Object> data = new HashMap<>(3);
                        data.put("planId", coupon.getPlanId());
                        data.put("subsStatus", SubsStatus.COUPONED);
                        data.put("subsRenewalAt", DateUtils.addDays(new Date(), coupon.getDays()));
                        log.info("Coupon {}, is issued for {}", coupon.getCode(), company.getName());
                        res[0] = new Response(data);
                      }
                    }
                  }
                  if (! res[0].isOK()) res[0] = Responses.DataProblem.DB_PROBLEM;

                } else {
                  res[0] = Responses.Already.ACTIVE_SUBSCRIPTION;
                }
              } else {
                res[0] = Responses.Illegal.COUPON_ISSUED_FOR_ANOTHER_COMPANY;
              }
            } else {
              res[0] = Responses.Already.USED_COUPON;
            }
          } else {
            res[0] = Responses.Invalid.COUPON;
          }
  
          return res[0].isOK();
        });

      }
    }

    return res[0];
  }

}
