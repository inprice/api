package io.inprice.api.app.voucher;

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
import io.inprice.common.models.Voucher;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.VoucherManager;

public class VoucherService {

  private static final Logger logger = LoggerFactory.getLogger(VoucherService.class);

  Response getVouchers() {
  	if (CurrentUser.getWorkspaceId() == null) return Responses.NotAllowed.NO_WORKSPACE;

    try (Handle handle = Database.getHandle()) {
      VoucherDao voucherDao = handle.attach(VoucherDao.class);

      List<Voucher> vouchers = voucherDao.findListByWorkspaceId(CurrentUser.getWorkspaceId());
    	if (CollectionUtils.isNotEmpty(vouchers)) {
        return new Response(vouchers);
      }
      return Responses.NotFound.VOUCHER;
    }
  }

  Response applyVoucher(String code) {
    Response response = Responses.Invalid.VOUCHER;

    if (VoucherManager.isValid(code)) {

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        VoucherDao voucherDao = handle.attach(VoucherDao.class);
        Voucher voucher = voucherDao.findByCode(code);

        if (voucher != null) {
          if (voucher.getIssuedAt() == null) {

            if (voucher.getIssuedId() == null || voucher.getIssuedId().equals(CurrentUser.getWorkspaceId())) {
              WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
              SystemDao planDao = handle.attach(SystemDao.class);

              Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
              if (workspace.getStatus().isOKForVoucher()) {

              	Plan voucherPlan = planDao.findById(voucher.getPlanId());
              	if (workspace.getLinkCount() == null) workspace.setLinkCount(0);

              	// if workspace's current link count is equal or less then voucher's limit
                if (workspace.getLinkCount().compareTo(voucherPlan.getLinkLimit()) <= 0) {
                  SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

                  boolean isOK = 
                    subscriptionDao.startFreeUseOrApplyVoucher(
                      CurrentUser.getWorkspaceId(),
                      WorkspaceStatus.VOUCHERED.name(),
                      voucherPlan.getId(),
                      voucher.getDays()
                    );

                  if (isOK) {
                    isOK = voucherDao.applyFor(voucher.getCode(), CurrentUser.getWorkspaceId());

                    if (isOK) {
                      WorkspaceTrans trans = new WorkspaceTrans();
                      trans.setWorkspaceId(CurrentUser.getWorkspaceId());
                      trans.setEventId(voucher.getCode());
                      trans.setEvent(SubsEvent.VOUCHER_USE_STARTED);
                      trans.setSuccessful(Boolean.TRUE);
                      trans.setDescription(voucher.getCode() + " is used.");

                      isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
                      if (isOK) {
                        isOK = 
                          workspaceDao.insertStatusHistory(
                            workspace.getId(), 
                            WorkspaceStatus.VOUCHERED.name(),
                            voucherPlan.getId()
                          );
                      }
                              
                      if (isOK) {
                        response = Commons.refreshSession(workspaceDao, workspace.getId());
                        logger.info("Voucher {}, is issued for {}", voucher.getCode(), workspace.getName());
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
              response = Responses.Illegal.VOUCHER_ISSUED_FOR_ANOTHER_WORKSPACE;
            }
          } else {
            response = Responses.Already.USED_VOUCHER;
          }
        } else {
          response = Responses.NotFound.VOUCHER;
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
