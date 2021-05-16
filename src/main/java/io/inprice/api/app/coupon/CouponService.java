package io.inprice.api.app.coupon;

import java.util.Collections;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.system.PlanDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.CouponManager;

public class CouponService {

  private static final Logger log = LoggerFactory.getLogger(CouponService.class);

  public Response createCoupon(Handle handle, long accountId, SubsEvent subsEvent, Integer planId, long days, String description) {
  	PlanDao planDao = handle.attach(PlanDao.class);
  	Plan plan = planDao.findById(planId);
  	if (plan != null) {

    	AccountDao accountDao = handle.attach(AccountDao.class);
    	Account account = accountDao.findById(accountId);
  		if (account != null) {
  			if (! AccountStatus.SUSPENDED.equals(account.getStatus())) {
  				
  	  		if (account.getUserCount() <= plan.getUserLimit() 
  	  				&& account.getLinkCount() <= plan.getLinkLimit() 
  	  				&& account.getAlarmCount() <= plan.getAlarmLimit()) {

  	  	    String couponCode = CouponManager.generate();
  	  	    CouponDao couponDao = handle.attach(CouponDao.class);
  	  	    boolean isOK = couponDao.create(
  	  	      couponCode,
  	  	      planId,
  	  	      days,
  	  	      description,
  	  	      accountId
  	  	    );

  	  	    if (isOK) {
  	  	      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
  	  	      AccountTrans trans = new AccountTrans();
  	  	      trans.setAccountId(accountId);
  	  	      trans.setEvent(subsEvent);
  	  	      trans.setSuccessful(Boolean.TRUE);
  	  	      trans.setReason(description);
  	  	      trans.setDescription("Issued coupon code: " + couponCode);
  	  	      subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
  	  	      return new Response(Collections.singletonMap("code", couponCode));
  	  	    } else {
  	  	    	return new Response("An error occurred, we will be looking for this!");
  	  	    }
  	  			
  				} else {
  					return new Response("Account current limits are greater than this plan's!");
  				}
  			} else {
  				return Responses.Illegal.SUSPENDED_ACCOUNT;
  			}
  		} else {
  			return Responses.NotFound.ACCOUNT;
  		}
  	}
    return Responses.NotFound.PLAN;
  }

  Response getCoupons() {
    try (Handle handle = Database.getHandle()) {
      CouponDao couponDao = handle.attach(CouponDao.class);

      List<Coupon> coupons = couponDao.findListByAccountId(CurrentUser.getAccountId());
      if (coupons != null && coupons.size() > 0) {
        return new Response(coupons);
      }
      return Responses.NotFound.COUPON;
    }
  }

  Response applyCoupon(String code) {
    Response response = Responses.Invalid.COUPON;

    if (CouponManager.isValid(code)) {

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        CouponDao couponDao = handle.attach(CouponDao.class);
        Coupon coupon = couponDao.findByCode(code);

        if (coupon != null) {
          if (coupon.getIssuedAt() == null) {

            if (coupon.getIssuedId() == null || coupon.getIssuedId().equals(CurrentUser.getAccountId())) {
              AccountDao accountDao = handle.attach(AccountDao.class);
              PlanDao planDao = handle.attach(PlanDao.class);

              Account account = accountDao.findById(CurrentUser.getAccountId());
              if (account.getStatus().isOKForCoupon()) {

              	Plan couponPlan = planDao.findById(coupon.getPlanId());
              	if (account.getLinkCount() == null) account.setLinkCount(0);

              	// if account's current link count is equal or less then coupon's limit
                if (account.getLinkCount().compareTo(couponPlan.getLinkLimit()) <= 0) {
                  SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

                  boolean isOK = 
                    subscriptionDao.startFreeUseOrApplyCoupon(
                      CurrentUser.getAccountId(),
                      AccountStatus.COUPONED.name(),
                      couponPlan.getId(),
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
                            couponPlan.getId()
                          );
                      }
                              
                      if (isOK) {
                        response = Commons.refreshSession(accountDao, account.getId());
                        log.info("Coupon {}, is issued for {}", coupon.getCode(), account.getName());
                      }
                    }
                  }
                  if (! response.isOK()) response = Responses.DataProblem.DB_PROBLEM;

                } else {
                  response = Responses.PermissionProblem.BROADER_PLAN_NEEDED;
                }
              } else {
                response = Responses.Already.ACTIVE_SUBSCRIPTION;
              }
            } else {
              response = Responses.Illegal.COUPON_ISSUED_FOR_ANOTHER_ACCOUNT;
            }
          } else {
            response = Responses.Already.USED_COUPON;
          }
        } else {
          response = Responses.Invalid.COUPON;
        }

        if (response.isOK())
        	handle.commit();
        else
        	handle.rollback();
      }
    }

    return response;
  }

}
