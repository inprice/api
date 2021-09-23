package io.inprice.api.app.credit;

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
import io.inprice.common.models.Credit;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.CreditManager;

public class CreditService {

  private static final Logger logger = LoggerFactory.getLogger(CreditService.class);

  Response getCredits() {
  	if (CurrentUser.getWorkspaceId() == null) return Responses.NotAllowed.NO_WORKSPACE;

    try (Handle handle = Database.getHandle()) {
      CreditDao creditDao = handle.attach(CreditDao.class);

      List<Credit> credits = creditDao.findListByWorkspaceId(CurrentUser.getWorkspaceId());
    	if (CollectionUtils.isNotEmpty(credits)) {
        return new Response(credits);
      }
      return Responses.NotFound.CREDIT;
    }
  }

  Response applyCredit(String code) {
    Response response = Responses.Invalid.CREDIT;

    if (CreditManager.isValid(code)) {

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        CreditDao creditDao = handle.attach(CreditDao.class);
        Credit credit = creditDao.findByCode(code);

        if (credit != null) {
          if (credit.getIssuedAt() == null) {

            if (credit.getIssuedId() == null || credit.getIssuedId().equals(CurrentUser.getWorkspaceId())) {
              WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
              SystemDao planDao = handle.attach(SystemDao.class);

              Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
              if (workspace.getStatus().isOKForCredit()) {

              	Plan creditPlan = planDao.findById(credit.getPlanId());
              	if (workspace.getLinkCount() == null) workspace.setLinkCount(0);

              	// if workspace's current link count is equal or less then credit's limit
                if (workspace.getLinkCount().compareTo(creditPlan.getLinkLimit()) <= 0) {
                  SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

                  boolean isOK = 
                    subscriptionDao.startFreeUseOrApplyCredit(
                      CurrentUser.getWorkspaceId(),
                      WorkspaceStatus.CREDITED.name(),
                      creditPlan.getId(),
                      credit.getDays()
                    );

                  if (isOK) {
                    isOK = creditDao.applyFor(credit.getCode(), CurrentUser.getWorkspaceId());

                    if (isOK) {
                      WorkspaceTrans trans = new WorkspaceTrans();
                      trans.setWorkspaceId(CurrentUser.getWorkspaceId());
                      trans.setEventId(credit.getCode());
                      trans.setEvent(SubsEvent.CREDIT_USE_STARTED);
                      trans.setSuccessful(Boolean.TRUE);
                      trans.setDescription(credit.getCode() + " is used.");

                      isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
                      if (isOK) {
                        isOK = 
                          workspaceDao.insertStatusHistory(
                            workspace.getId(), 
                            WorkspaceStatus.CREDITED.name(),
                            creditPlan.getId()
                          );
                      }
                              
                      if (isOK) {
                        response = Commons.refreshSession(workspaceDao, workspace.getId());
                        logger.info("Credit {}, is issued for {}", credit.getCode(), workspace.getName());
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
              response = Responses.Illegal.CREDIT_ISSUED_FOR_ANOTHER_WORKSPACE;
            }
          } else {
            response = Responses.Already.USED_CREDIT;
          }
        } else {
          response = Responses.NotFound.CREDIT;
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
