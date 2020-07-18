package io.inprice.api.app.subscription;

import io.inprice.api.app.coupon.CouponRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.helpers.Beans;
import io.inprice.common.utils.CouponManager;

public class SubscriptionService {

  private static final SubscriptionRepository repository = Beans.getSingleton(SubscriptionRepository.class);
  private final CouponRepository couponRepository = Beans.getSingleton(CouponRepository.class);

  public ServiceResponse getTransactions() {
    return repository.getTransactions();
  }

  public ServiceResponse cancel() {
    return repository.cancel();
  }

  public ServiceResponse getCoupons() {
    return couponRepository.getCoupons();
  }

  public ServiceResponse applyCoupon(String code) {
    if (! CouponManager.isValid(code)) return Responses.Invalid.COUPON;
    return couponRepository.applyCoupon(code);
  }

}
