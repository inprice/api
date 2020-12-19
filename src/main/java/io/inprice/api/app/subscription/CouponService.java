package io.inprice.api.app.subscription;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.CouponManager;

class CouponService {

  private static final Logger log = LoggerFactory.getLogger(CouponService.class);

  Response getCoupons() {
    try (Handle handle = Database.getHandle()) {
      CouponDao couponDao = handle.attach(CouponDao.class);

      List<Coupon> coupons = couponDao.findListByIssuedAccountId(CurrentUser.getAccountId());
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
        handle.inTransaction(transactional -> {
          CouponDao couponDao = transactional.attach(CouponDao.class);

          Coupon coupon = couponDao.findByCode(code);
          if (coupon != null) {
            if (coupon.getIssuedAt() == null) {

              if (coupon.getIssuedAccountId() == null || coupon.getIssuedAccountId().equals(CurrentUser.getAccountId())) {
                AccountDao accountDao = transactional.attach(AccountDao.class);

                Account account = accountDao.findById(CurrentUser.getAccountId());
                if (account.getStatus().isOKForCoupon()) {

                  Plan selectedPlan = null;
                  Plan couponPlan = Plans.findByName(coupon.getPlanName());
                  Plan accountPlan = (account.getPlanName() != null ? Plans.findByName(account.getPlanName()) : null);

                  if (accountPlan == null || couponPlan.getId() > accountPlan.getId()) {
                    selectedPlan = couponPlan;
                  }

                  // only broader plan transitions allowed
                  if (account.getProductCount().compareTo(selectedPlan.getProductLimit()) <= 0) {
                    SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

                    boolean isOK = 
                      subscriptionDao.startFreeUseOrApplyCoupon(
                        CurrentUser.getAccountId(),
                        AccountStatus.COUPONED.name(),
                        selectedPlan.getName(),
                        selectedPlan.getProductLimit(), 
                        coupon.getDays()
                      );

                    if (isOK) {
                      isOK = couponDao.applyFor(coupon.getCode(), CurrentUser.getAccountId());

                      if (isOK) {
                        AccountTrans trans = new AccountTrans();
                        trans.setAccountId(CurrentUser.getAccountId());
                        trans.setEventId(coupon.getCode());
                        trans.setEvent(SubsEvent.COUPON_USE_STARTED);
                        trans.setSuccessful(Boolean.TRUE);
                        trans.setDescription(coupon.getCode() + " is used.");

                        isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
                        if (isOK) {
                          isOK = 
                            accountDao.insertStatusHistory(
                              account.getId(), 
                              AccountStatus.COUPONED.name(),
                              selectedPlan.getName(),
                              null, null
                            );
                        }
                                
                        if (isOK) {
                          res[0] = Commons.refreshSession(accountDao, account.getId());
                          log.info("Coupon {}, is issued for {}", coupon.getCode(), account.getName());
                        }
                      }
                    }
                    if (! res[0].isOK()) res[0] = Responses.DataProblem.DB_PROBLEM;

                  } else {
                    res[0] = Responses.PermissionProblem.BROADER_PLAN_NEEDED;
                  }
                } else {
                  res[0] = Responses.Already.ACTIVE_SUBSCRIPTION;
                }
              } else {
                res[0] = Responses.Illegal.COUPON_ISSUED_FOR_ANOTHER_ACCOUNT;
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
