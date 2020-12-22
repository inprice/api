package io.inprice.api.app.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerUpdateParams;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Plan;

class SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  private final StripeService stripeService = Beans.getSingleton(StripeService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer templateRenderer = Beans.getSingleton(TemplateRenderer.class);

  Response createCheckout(int planId) {
    return stripeService.createCheckout(planId);
  }

  Response getTransactions() {
    Map<String, Object> data = new HashMap<>(3);

    try (Handle handle = Database.getHandle()) {
      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

      List<AccountTrans> allTrans = subscriptionDao.findListByAccountId(CurrentUser.getAccountId());
      data.put("all", allTrans);

      if (allTrans != null && allTrans.size() > 0) {
        List<AccountTrans> invoiceTrans = new ArrayList<>();
        for (AccountTrans st : allTrans) {
          if (st.getFileUrl() != null) {
            invoiceTrans.add(st);
          }
        }
        data.put("invoice", invoiceTrans);
      }
    }

    return new Response(data);
  }

  Response getCurrentAccount() {
    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      return new Response(accountDao.findById(CurrentUser.getAccountId()));
    }
  }

  Response cancel() {
    Response[] res = { Responses.DataProblem.SUBSCRIPTION_PROBLEM };

    Account[] account = { null };

    //stripeService will create its own transaction. 
    //so, we must not extend/cascade any db connection, thus, a new one is handled here in a separated block
    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      account[0] = accountDao.findById(CurrentUser.getAccountId());
    }

    if (CurrentUser.getUserId().equals(account[0].getAdminId())) {
      if (account[0].getStatus().isOKForCancel()) {

        if (AccountStatus.SUBSCRIBED.equals(account[0].getStatus())) { //if a subscriber
          res[0] = stripeService.cancel(account[0]);
        } else { //free or couponed user

          try (Handle handle = Database.getHandle()) {
            handle.inTransaction(transactional -> {
              SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);
      
              boolean isOK = subscriptionDao.terminate(account[0].getId(), AccountStatus.CANCELLED.name());
              if (isOK) {

                AccountTrans trans = new AccountTrans();
                trans.setAccountId(account[0].getId());
                trans.setSuccessful(Boolean.TRUE);
                trans.setDescription(("Manual cancelation."));
                trans.setEvent(AccountStatus.COUPONED.equals(account[0].getStatus()) ? SubsEvent.COUPON_USE_CANCELLED : SubsEvent.FREE_USE_CANCELLED);

                isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
      
                if (isOK) {
                  AccountDao accountDao = transactional.attach(AccountDao.class);
                  isOK = 
                    accountDao.insertStatusHistory(
                      account[0].getId(),
                      AccountStatus.CANCELLED.name(),
                      account[0].getPlanName(),
                      null, null
                    );
                  if (isOK) {
                    Map<String, Object> mailMap = new HashMap<>(2);
                    mailMap.put("user", CurrentUser.getEmail());
                    mailMap.put("account", StringUtils.isNotBlank(account[0].getTitle()) ? account[0].getTitle() : account[0].getName());
                    String message = templateRenderer.render(EmailTemplate.FREE_ACCOUNT_CANCELLED, mailMap);
                    emailSender.send(
                      Props.APP_EMAIL_SENDER(), 
                      "Notification about your cancelled plan in inprice.", CurrentUser.getEmail(), 
                      message
                    );

                    res[0] = Responses.OK;
                  }
                }
              }
              return res[0].isOK();
            });
          }
        }

      } else {
        res[0] = Responses.Illegal.NOT_SUITABLE_FOR_CANCELLATION;
      }
    } else {
      res[0] = Responses._403;
    }

    if (res[0].isOK()) {
      res[0] = Commons.refreshSession(account[0].getId());
    }

    return res[0];
  }

  Response startFreeUse() {
    Response[] res = { Responses.DataProblem.SUBSCRIPTION_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {
        AccountDao accountDao = transactional.attach(AccountDao.class);

        Account account = accountDao.findById(CurrentUser.getAccountId());
        if (account != null) {

          if (!account.getStatus().equals(AccountStatus.FREE)) {
            if (account.getStatus().isOKForFreeUse()) {
              SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

              Plan basicPlan = Plans.findById(0); // Basic Plan

              boolean isOK = 
                subscriptionDao.startFreeUseOrApplyCoupon(
                  CurrentUser.getAccountId(),
                  AccountStatus.FREE.name(),
                  basicPlan.getName(),
                  basicPlan.getProductLimit(),
                  Props.APP_DAYS_FOR_FREE_USE()
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
                      basicPlan.getName(),
                      null, null
                    );
                }
              }

              if (isOK) {
                res[0] = Commons.refreshSession(accountDao, account.getId());
              }

            } else {
              res[0] = Responses.Illegal.NO_FREE_USE_RIGHT;
            }
          } else {
            res[0] = Responses.Already.IN_FREE_USE;
          }
        } else {
          res[0] = Responses.NotFound.ACCOUNT;
        }

        return res[0].equals(Responses.OK);
      });
    }

    return res[0];
  }

  Response saveInfo(CustomerDTO dto) {
    Response res = new Response("Sorry, we are unable to update your invoice info at the moment. We are working on it.");

    String problem = validateInvoiceInfo(dto);
    if (problem == null) {

      Account account = null;
      try (Handle handle = Database.getHandle()) {
        AccountDao dao = handle.attach(AccountDao.class);
        account = dao.findById(CurrentUser.getAccountId());
      }

      if (account != null) {
        if (StringUtils.isNotBlank(account.getCustId())) {

          CustomerUpdateParams.Address address =
            CustomerUpdateParams.Address.builder()
              .setLine1(dto.getAddress1())
              .setLine2(dto.getAddress2())
              .setPostalCode(dto.getPostcode())
              .setCity(dto.getCity())
              .setState(dto.getState())
              .setCountry(dto.getCountry())
            .build();

          CustomerUpdateParams customerParams =
            CustomerUpdateParams.builder()
              .setName(dto.getTitle())
              .setEmail(CurrentUser.getEmail())
              .setAddress(address)
            .build();

          Customer customer = null;

          try {
            customer = Customer.retrieve(account.getCustId()).update(customerParams);
            if (customer != null) {
              res = Responses.OK;
              log.info(CurrentUser.getAccountName() + " customer info is updated, Id: " + customer.getId());
              log.info("Customer info is updated. Account: {}, Subs Customer Id: {}, Title: {}, Email: {}", 
                CurrentUser.getAccountName(), customer.getId(), dto.getTitle(), CurrentUser.getEmail());
            } else {
              log.error("Failed to update a new customer in Stripe.");
            }

          } catch (StripeException e) {
            log.error("Failed to update a new customer in Stripe", e);
            log.error("Account: {}, Title: {}, Email: {}", CurrentUser.getAccountName(), dto.getTitle(), CurrentUser.getEmail());
          }

          if (res.isOK() && customer != null) {
            dto.setCustId(customer.getId());

            try (Handle handle = Database.getHandle()) {
              AccountDao dao = handle.attach(AccountDao.class);
              boolean isOK = dao.update(dto, CurrentUser.getAccountId());
              if (isOK) {
                res = Responses.OK;
              } else {
                res = Responses.NotFound.ACCOUNT;
              }
            }
          }
        } else {
          res = Responses.Already.PASSIVE_SUBSCRIPTION;
        }
      } else {
        res = Responses.NotFound.ACCOUNT;
      }

    } else {
      res = new Response(problem);
    }

    return res;
  }

  private String validateInvoiceInfo(CustomerDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid customer data!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getTitle())) {
        problem = "Title cannot be null!";
      } else if (dto.getTitle().length() < 3 || dto.getTitle().length() > 255) {
        problem = "Title must be between 3 - 255 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getAddress1())) {
        problem = "Address line 1 cannot be null!";
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
        problem = "City cannot be null!";
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
        problem = "Country code cannot be null!";
      } else if (dto.getCountry().length() != 2) {
        problem = "Country code must be 2 chars";
      }
    }

    return problem;
  }

}
