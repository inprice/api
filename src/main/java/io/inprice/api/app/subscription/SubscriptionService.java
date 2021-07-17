package io.inprice.api.app.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.system.PlanDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.UserMarkType;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Plan;
import io.inprice.common.models.UserMark;

class SubscriptionService {

  //private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  //private final StripeService stripeService = Beans.getSingleton(StripeService.class);
  private final RedisClient redis = Beans.getSingleton(RedisClient.class);
	
  Response createCheckout(int planId) {
    //return stripeService.createCheckout(planId);
  	return Responses.BAD_REQUEST;
  }

  Response cancel() {
    Response response = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    Account account = null;

    //stripeService will create its own handle. 
    //so, we must not extend/cascade any db connection, thus, a new one is handled here in a separated block
    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      account = accountDao.findById(CurrentUser.getAccountId());
    }

    if (CurrentUser.getUserId().equals(account.getAdminId())) {
      if (account.getStatus().isOKForCancel()) {

        if (AccountStatus.SUBSCRIBED.equals(account.getStatus())) { //if a subscriber
          //response = stripeService.cancel(account);
        } else { //free or couponed user

          try (Handle handle = Database.getHandle()) {
          	handle.begin();

            SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
            boolean isOK = subscriptionDao.terminate(account.getId(), AccountStatus.CANCELLED.name());
            if (isOK) {

              AccountTrans trans = new AccountTrans();
              trans.setAccountId(account.getId());
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("Manual cancelation."));
              trans.setEvent(AccountStatus.COUPONED.equals(account.getStatus()) ? SubsEvent.COUPON_USE_CANCELLED : SubsEvent.FREE_USE_CANCELLED);

              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
    
              if (isOK) {
                AccountDao accountDao = handle.attach(AccountDao.class);
                isOK = 
                  accountDao.insertStatusHistory(
                    account.getId(),
                    AccountStatus.CANCELLED.name(),
                    account.getPlanId()
                  );
                if (isOK) {
                  Map<String, Object> mailMap = new HashMap<>(2);
                  mailMap.put("user", CurrentUser.getEmail());
                  mailMap.put("account", StringUtils.isNotBlank(account.getTitle()) ? account.getTitle() : account.getName());
                  
                  redis.sendEmail(
              			EmailData.builder()
                			.template(EmailTemplate.FREE_ACCOUNT_CANCELLED)
                			.from(Props.APP_EMAIL_SENDER)
                			.to(CurrentUser.getEmail())
                			.subject("Notification about your cancelled plan in inprice.")
                			.data(mailMap)
                		.build()	
              		);

                  response = Responses.OK;
                }
              }
            }

            if (response.isOK())
            	handle.commit();
            else
            	handle.rollback();
          }
        }

      } else {
        response = Responses.Illegal.NOT_SUITABLE_FOR_CANCELLATION;
      }
    } else {
      response = Responses._403;
    }

    if (response.isOK()) {
      response = Commons.refreshSession(account.getId());
    }

    return response;
  }

  Response startFreeUse() {
    Response response = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    try (Handle handle = Database.getHandle()) {

      AccountDao accountDao = handle.attach(AccountDao.class);
      UserMark um_FREE_USE = accountDao.getUserMarkByEmail(CurrentUser.getEmail(), UserMarkType.FREE_USE);
      if (um_FREE_USE == null || um_FREE_USE.getWhitelisted().equals(Boolean.TRUE)) {

        Account account = accountDao.findById(CurrentUser.getAccountId());
        if (account != null) {

          if (account.getStatus().isOKForFreeUse()) {
          	PlanDao planDao = handle.attach(PlanDao.class);
            SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

            Plan basicPlan = planDao.findByName("Basic Plan");

          	handle.begin();
            
            boolean isOK = 
              subscriptionDao.startFreeUseOrApplyCoupon(
                CurrentUser.getAccountId(),
                AccountStatus.FREE.name(),
                basicPlan.getId(),
                Props.APP_DAYS_FOR_FREE_USE
              );

            if (isOK) {
              AccountTrans trans = new AccountTrans();
              trans.setAccountId(CurrentUser.getAccountId());
              trans.setEvent(SubsEvent.FREE_USE_STARTED);
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("Free subscription has been started."));
              
              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                isOK = 
                  accountDao.insertStatusHistory(
                    account.getId(),
                    AccountStatus.FREE.name(),
                    basicPlan.getId()
                  );
                if (um_FREE_USE == null) accountDao.addUserMark(CurrentUser.getEmail(), UserMarkType.FREE_USE);
              }
            }

            if (isOK) {
              response = Commons.refreshSession(accountDao, account.getId());
              handle.commit();
            } else {
            	handle.rollback();
            }

          } else {
            response = Responses.Illegal.NO_FREE_USE_RIGHT;
          }
        } else {
        	response = Responses.NotFound.ACCOUNT;
        }
      } else {
        response = Responses.Already.FREE_USE_USED;
      }
    }

    return response;
  }

  Response getInfo() {
    Map<String, Object> data = new HashMap<>(3);

    try (Handle handle = Database.getHandle()) {
      AccountDao dao = handle.attach(AccountDao.class);
      Account account = dao.findById(CurrentUser.getAccountId());
      if (account != null) {
      	Map<String, Object> info = new HashMap<>(7);
      	info.put("title", account.getTitle());
      	info.put("address1", account.getAddress1());
      	info.put("address2", account.getAddress2());
      	info.put("postcode", account.getPostcode());
      	info.put("city", account.getCity());
      	info.put("state", account.getState());
      	info.put("country", account.getCountry());
      	data.put("info", info);
      	
        SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
        
        List<AccountTrans> allTrans = subscriptionDao.findListByAccountId(CurrentUser.getAccountId());
        data.put("transactions", allTrans);
  
        if (allTrans != null && allTrans.size() > 0) {
          List<AccountTrans> invoiceTrans = new ArrayList<>();
          for (AccountTrans st : allTrans) {
            if (st.getFileUrl() != null) {
              invoiceTrans.add(st);
            }
          }
          data.put("invoices", invoiceTrans);
        }
      } else {
      	return Responses.NotFound.ACCOUNT;
      }
    }

    return new Response(data);
  }

  Response saveInfo(CustomerDTO dto) {
  	Response res = Responses.NotFound.ACCOUNT;

    String problem = validateInvoiceInfo(dto);

    if (problem == null) {
      Account account = null;
      try (Handle handle = Database.getHandle()) {
        AccountDao dao = handle.attach(AccountDao.class);
        account = dao.findById(CurrentUser.getAccountId());

        if (account != null) {
          boolean isOK = dao.update(dto, CurrentUser.getAccountId());
          if (isOK) {
            res = Responses.OK;
          } else {
          	res = Responses.DataProblem.DB_PROBLEM;
          }
        }
      }
    } else {
      res = new Response(problem);
    }

    return res;
  }

  private String validateInvoiceInfo(CustomerDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getTitle())) {
      problem = "Title cannot be empty!";
    } else if (dto.getTitle().length() < 3 || dto.getTitle().length() > 255) {
      problem = "Title must be between 3 - 255 chars";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getAddress1())) {
        problem = "Address line 1 cannot be empty!";
      } else if (dto.getAddress1().length() > 255) {
        problem = "Address line 1 must be less than 255 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isNotBlank(dto.getAddress1()) && dto.getAddress1().length() > 255) {
        problem = "Address line 2 must be less than 256 chars";
      }
    }

    if (problem == null) {
      if (! StringUtils.isNotBlank(dto.getPostcode()) && dto.getPostcode().length() < 3 || dto.getPostcode().length() > 8) {
        problem = "Postcode must be between 3-8 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCity())) {
        problem = "City cannot be empty!";
      } else if (dto.getCity().length() < 2 || dto.getCity().length() > 8) {
        problem = "City must be between 2-70 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isNotBlank(dto.getState()) && dto.getState().length() > 70) {
        problem = "State must be less than 71 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCountry())) {
        problem = "Country code cannot be empty!";
      } else if (dto.getCountry().length() != 2) {
        problem = "Country code must be 2 chars";
      }
    }

    return problem;
  }

}
