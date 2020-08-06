package io.inprice.api.app.coupon;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.helpers.Beans;
import io.inprice.common.utils.CouponManager;

public class CouponService {

  private static final CouponRepository repository = Beans.getSingleton(CouponRepository.class);

  public ServiceResponse getCoupons() {
    return repository.getCoupons();
  }

  public ServiceResponse applyCoupon(String code) {
    if (!CouponManager.isValid(code)) return Responses.Invalid.COUPON;
    return repository.applyCoupon(code);
  }

}
