package io.inprice.api.app.coupon;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.system.SystemDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.WorkspaceTrans;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.CouponManager;

public class CouponService {

  private static final Logger logger = LoggerFactory.getLogger(CouponService.class);

  Response getCoupons() {
  	if (CurrentUser.getWorkspaceId() == null) return Responses.NotAllowed.NO_WORKSPACE;

    try (Handle handle = Database.getHandle()) {
      CouponDao couponDao = handle.attach(CouponDao.class);

      List<Coupon> coupons = couponDao.findListByWorkspaceId(CurrentUser.getWorkspaceId());
    	if (CollectionUtils.isNotEmpty(coupons)) {
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

            if (coupon.getIssuedId() == null || coupon.getIssuedId().equals(CurrentUser.getWorkspaceId())) {
              WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
              SystemDao planDao = handle.attach(SystemDao.class);

              Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
              if (workspace.getStatus().isOKForCoupon()) {

              	Plan couponPlan = planDao.findById(coupon.getPlanId());
              	if (workspace.getLinkCount() == null) workspace.setLinkCount(0);

              	// if workspace's current link count is equal or less then coupon's limit
                if (workspace.getLinkCount().compareTo(couponPlan.getLinkLimit()) <= 0) {
                  SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

                  boolean isOK = 
                    subscriptionDao.startFreeUseOrApplyCoupon(
                      CurrentUser.getWorkspaceId(),
                      WorkspaceStatus.COUPONED.name(),
                      couponPlan.getId(),
                      coupon.getDays()
                    );

                  if (isOK) {
                    isOK = couponDao.applyFor(coupon.getCode(), CurrentUser.getWorkspaceId());

                    if (isOK) {
                      WorkspaceTrans trans = new WorkspaceTrans();
                      trans.setWorkspaceId(CurrentUser.getWorkspaceId());
                      trans.setEventId(coupon.getCode());
                      trans.setEvent(SubsEvent.COUPON_USE_STARTED);
                      trans.setSuccessful(Boolean.TRUE);
                      trans.setDescription(coupon.getCode() + " is used.");

                      isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
                      if (isOK) {
                        isOK = 
                          workspaceDao.insertStatusHistory(
                            workspace.getId(), 
                            WorkspaceStatus.COUPONED.name(),
                            couponPlan.getId()
                          );
                      }
                              
                      if (isOK) {
                        response = Commons.refreshSession(workspaceDao, workspace.getId());
                        logger.info("Coupon {}, is issued for {}", coupon.getCode(), workspace.getName());
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
              response = Responses.Illegal.COUPON_ISSUED_FOR_ANOTHER_WORKSPACE;
            }
          } else {
            response = Responses.Already.USED_COUPON;
          }
        } else {
          response = Responses.NotFound.COUPON;
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
