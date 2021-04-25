package io.inprice.api.app.subscription;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Address;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.coupon.CouponService;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.helpers.CodeGenerator;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.CheckoutStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Checkout;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.DateUtils;

class StripeService {

  private static final Logger log = LoggerFactory.getLogger(StripeService.class);

  private final CouponService couponService  = Beans.getSingleton(CouponService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer templateRenderer = Beans.getSingleton(TemplateRenderer.class);

  /**
   * To get a payment, creates checkout session
   * 
   * @param planId
   */
  Response createCheckout(int planId) {
    Plan plan = Plans.findById(planId);

    if (plan != null) {

      String checkoutHash = CodeGenerator.hash();

      try (Handle handle = Database.getHandle()) {
        AccountDao accountDao = handle.attach(AccountDao.class);
        Account account = accountDao.findById(CurrentUser.getAccountId());

        //check if the last checkout is active
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        Checkout checkout = checkoutDao.findCheckedAtout(CurrentUser.getAccountId());
        if (checkout != null) {
          String message = null;
          if (CheckoutStatus.PENDING.equals(checkout.getStatus())) {
            //must not be greater than 2 hours thanks to PendingCheckoutsCloser!!!
            long diff = DateUtils.findHourDiff(checkout.getCreatedAt(), new Date());
            String delay = null;
            if (diff == 0) {
              delay = " a couple of minutes";
            } else if (diff > 0 && diff <= 2) {
              delay = (diff == 1 ? " one hour more" : " two hours more");
            } else { // normally this cannot be happened
              String checkoutStatus = retrieveCheckoutStatus(checkoutHash, checkout.getSessionId());
              message = "A communication problem occurred with Stripe (the payment provider). We are working on this." ;
              log.error("Communication problem, checkout is still in PENDING state for a long time! Hash: {}, Status: {}", checkoutHash, checkoutStatus);
            }
            if (delay != null) {
              message = "You have an active checkout. Please wait " + delay + " for it to complete.";
            } else {
              message = "Your last checkout is still in PENDING state which is unexpected. We will manage this in a short while.";
            }
          } else if (CheckoutStatus.SUCCESSFUL.equals(checkout.getStatus()) && AccountStatus.SUBSCRIBED.equals(account.getStatus())) {
            message = "Your subscription and (last) checkout are successful. You cannot create a new checkout!";
          }
          if (message != null) return new Response(message);
        }

        // only broader plan transitions allowed
        if (account.getLinkCount().compareTo(plan.getLinkLimit()) > 0) {
          return Responses.PermissionProblem.BROADER_PLAN_NEEDED;
        }
      }

      SessionCreateParams params = SessionCreateParams.builder()
        .setCustomerEmail(CurrentUser.getEmail())
        .setClientReferenceId(CurrentUser.getAccountId().toString())
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
        .addLineItem(
          SessionCreateParams.LineItem
            .builder()
              .setQuantity(1L)
              .setPrice(plan.getStripePriceId())
              .setDescription(plan.getFeatures().get(0) + " Monthly subscription.")
            .build()
        )
        .setSubscriptionData(
          SessionCreateParams.SubscriptionData
            .builder()
              .putMetadata("hash", checkoutHash)
            .build()
        )
        .setSuccessUrl(
          String.format(
            "%s/%d/app/payment-ok/%s", Props.APP_WEB_URL(), CurrentUser.getSessionNo(), checkoutHash
          )
        )
        .setCancelUrl(
          String.format(
            "%s/%d/app/payment-cancel/%s", Props.APP_WEB_URL(), CurrentUser.getSessionNo(), checkoutHash
          )
        )
      .build();

      try (Handle handle = Database.getHandle()) {
        Session session = Session.create(params);

        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        boolean isOK = checkoutDao.insert(checkoutHash, session.getId(), CurrentUser.getAccountId(), plan.getName());
        if (isOK) {
          Map<String, Object> dataMap = new HashMap<>(2);
          dataMap.put("hash", checkoutHash);
          dataMap.put("sessionId", session.getId());
          return new Response(dataMap);
        } else {
          log.error("Failed to insert checkout!");
          return Responses.ServerProblem.CHECKOUT_PROBLEM;
        }

      } catch (StripeException e) {
        log.error("Failed to create checkout session", e);
        return Responses.ServerProblem.CHECKOUT_PROBLEM;
      }
    }

    return Responses.NotFound.PLAN;
  }

  Response cancelCheckout(String checkoutHash) {
    try (Handle handle = Database.getHandle()) {
      if (StringUtils.isNotBlank(checkoutHash)) {
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        Checkout checkout = checkoutDao.findByHash(checkoutHash);
        if (checkout != null) {
          if (! checkoutDao.update(checkoutHash, CheckoutStatus.CANCELLED.name(), "Cancelled by user.")) {
            log.error("Failed to update checkout! Hash: {}", checkoutHash);
          }
        } else {
          log.error("Failed to cancel checkout! Hash: {}", checkoutHash);
        }
      } else {
        log.error("Failed to get checkout info! Hash is null.");
      }
    }
    return Responses.OK;
  }

  private String retrieveCheckoutStatus(String checkoutHash, String sessionId) {
    try {
      Session session = Session.retrieve(sessionId);
      if (session != null) return session.getPaymentStatus();
    } catch (Exception e) {
      log.error("Failed to retrieve a checkout status. Hash: " + checkoutHash, e);
    }
    return null;
  }

  /**
   * Cancel operations should be taken into account immediately.
   * Instead of waiting cancel hook in handleHookEvent, we manage cancelation things in this method.
   * 
   */
  Response cancel(Account account) {
    boolean isAlreadyCancelled = false;
    Subscription subsResult = null;
    try {
      Subscription subscription = Subscription.retrieve(account.getSubsId());
      subsResult = subscription.cancel();
    } catch (InvalidRequestException e) {
      isAlreadyCancelled = e.getMessage().indexOf("No such subscription") > -1;
    } catch (Exception e) {
      log.error("Failed to cancel subs", e);
    }

    // if a subscriber cancels while he/she has more than more than 3 days to renewal,
    // he/she will be given a coupon to amortise those days
    if (isAlreadyCancelled || subsResult != null) {
      if (isAlreadyCancelled || subsResult.getStatus().equals("canceled")) {

        CustomerDTO dto = new CustomerDTO();
        dto.setCustId(account.getCustId());
        dto.setSubsId(account.getSubsId());
        dto.setPlanName(account.getPlanName());

        AccountTrans trans = new AccountTrans();
        trans.setAccountId(account.getId());
        trans.setEventId(subsResult.getId());
        trans.setEvent(SubsEvent.SUBSCRIPTION_CANCELLED);
        trans.setSuccessful(Boolean.TRUE);
        trans.setDescription("Manuel cancel.");
        return addhandle(account.getId(), account.getCustId(), dto, trans);

      } else if (subsResult != null) {
        log.error("Unexpected subs status: {}", subsResult.getStatus());
      }
    } else {
      log.error("Not a cancelled subscription and subsResult is null!");
    }
    log.error("Subs cancellation problem for Account: {}", account.getId());
    return Responses.DataProblem.SUBSCRIPTION_PROBLEM;
  }

  /**
   * Upgrades or Downgrads the plan
   * 
   */
  Response changePlan(int newPlanId) {
    Response res = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    Account account = null;

    try (Handle handle = Database.getHandle()) {
      AccountDao accountDao = handle.attach(AccountDao.class);
      account = accountDao.findById(CurrentUser.getAccountId());
    }

    if (account != null) {
      if (AccountStatus.SUBSCRIBED.equals(account.getStatus())) {
        if (StringUtils.isNotBlank(account.getSubsId())) {

          Plan newPlan = Plans.findById(newPlanId);
          if (newPlan != null) {

            if (! newPlan.getName().equals(account.getPlanName())) {
              boolean noNeedABroaderPlan =
                (account.getLinkCount() == 0) || (account.getLinkCount().compareTo(newPlan.getLinkLimit()) <= 0);

              if (noNeedABroaderPlan) {
                try {
                  Subscription subscription = Subscription.retrieve(account.getSubsId());

                  SubscriptionUpdateParams params =
                    SubscriptionUpdateParams.builder()
                      .setPaymentBehavior(SubscriptionUpdateParams.PaymentBehavior.PENDING_IF_INCOMPLETE)
                      .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                      .addItem(
                        SubscriptionUpdateParams.Item.builder()
                          .setId(subscription.getItems().getData().get(0).getId())
                          .setPrice(newPlan.getStripePriceId())
                          .build())
                      .build();
                  
                  Subscription subs = subscription.update(params);
                  Invoice invoice = Invoice.retrieve(subs.getLatestInvoice());

                  if (subs.getPendingUpdate() == null) {
                    res = Commons.refreshSession(account.getId());
                  } else {
                    invoice.voidInvoice();
                    res = Responses.NotSuitable.PAYMENT_FAILURE_ON_PLAN_CHANGE;
                  }

                } catch (Exception e) {
                  log.error("Failed to change plan. Account: " + account.getId() + ", New Plan: " + newPlanId, e);
                  res = Responses.ServerProblem.EXCEPTION;
                }
              } else {
                res = Responses.PermissionProblem.BROADER_PLAN_NEEDED;
              }
            } else {
              res = Responses.Already.HAS_THE_SAME_PLAN;
            }
          } else {
            res = Responses.NotFound.PLAN;
          }
        } else {
          res = Responses.NotFound.SUBSCRIPTION;
        }
      } else {
        res = Responses.NotSuitable.PLAN_CHANGE;
      }
    } else {
      res = Responses.NotFound.ACCOUNT;
    }

    return res;
  }

  /**
   * Handles failed and successful invoice handles to manage subscriptions
   * 
   * @param event
   */
  Response handleHookEvent(Event event) {
    Response res = Responses.BAD_REQUEST;

    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    StripeObject stripeObject = null;
    if (dataObjectDeserializer.getObject().isPresent()) {
      stripeObject = dataObjectDeserializer.getObject().get();
    } else {
      log.error("Stripe version problem. Event: {}", event.getType());
      return Responses.BAD_REQUEST;
    }

    if (stripeObject != null) {

      switch (event.getType()) {

        case "invoice.payment_succeeded": {
          Invoice invoice = (Invoice) stripeObject;
          List<InvoiceLineItem> invoiceItemList = invoice.getLines().getData();
          InvoiceLineItem firstItem = invoiceItemList.get(0);

          CustomerDTO dto = generateCustDTO(invoice);
          dto.setRenewalDate(new java.sql.Timestamp(firstItem.getPeriod().getEnd() * 1000));          

          Long accountId = null;
          SubsEvent subsEvent = null;
          boolean successful = false;
          String description = null;

          try {

            switch (invoice.getBillingReason()) {

              //a new subscriber
              case "subscription_create": {
                //the checkout must be completed
                try (Handle handle = Database.getHandle()) {
                  CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
                  String checkoutHash = firstItem.getMetadata().get("hash");
                  if (StringUtils.isNotBlank(checkoutHash)) {
                    Checkout checkout = checkoutDao.findByHash(checkoutHash);
                    if (checkout != null) {
                      if (checkoutDao.update(checkoutHash, CheckoutStatus.SUCCESSFUL.name(), null)) {
                        successful = true;
                        accountId = checkout.getAccountId();
                        subsEvent = SubsEvent.SUBSCRIPTION_STARTED;
                        description = firstItem.getDescription();
                        dto.setPlanName(checkout.getPlanName());
                      } else {
                        log.error("Failed to update checkout! Hash: {}", checkoutHash);
                      }
                    } else {
                      log.error("Failed to finish checkout! Hash: {}", checkoutHash);
                    }
                  } else {
                    log.error("Failed to get checkout info! Hash is null.");
                  }
                }
                break;
              }

              //renewal
              case "subscription_cycle": {
                try (Handle handle = Database.getHandle()) {
                  AccountDao accountDao = handle.attach(AccountDao.class);
                  Account account = accountDao.findByCustId(invoice.getCustomer());
                  if (account != null) {
                    successful = true;
                    accountId = account.getId();
                    subsEvent = SubsEvent.SUBSCRIPTION_RENEWED;
                    description = firstItem.getDescription();
                  } else {
                    log.error("invoice.payment_succeeded & subscription_cycle: failed to find account by CustId: {}", invoice.getCustomer());
                  }
                }
                break;
              }

              //plan changing
              case "subscription_update": {
                Account account = null;

                try (Handle handle = Database.getHandle()) {
                  AccountDao accountDao = handle.attach(AccountDao.class);
                  account = accountDao.findByCustId(invoice.getCustomer());
                }

                if (account != null) {
                  if (invoice.getStatus().equals("paid")) {

                    Plan newPlan = null;
                    InvoiceLineItem foundItem = null;
                    for (InvoiceLineItem ili : invoiceItemList) {
                      if (ili.getPrice() != null) {
                        Plan iliPlan = Plans.findByPriceId(ili.getPrice().getId());
                        if (! iliPlan.getName().equals(account.getPlanName())) {
                          foundItem = ili;
                          newPlan = iliPlan;
                          break;
                        }
                      }
                    }

                    if (newPlan != null) {
                      successful = true;
                      accountId = account.getId();
                      subsEvent = SubsEvent.SUBSCRIPTION_CHANGED;
                      description = foundItem.getDescription();
                      dto.setPlanName(newPlan.getName());
                    } else {
                      log.error("invoice.payment_succeeded & subscription_update: failed to find plan for CustId: {}", invoice.getCustomer());
                    }
                  } else {
                    accountId = account.getId();
                    subsEvent = SubsEvent.SUBSCRIPTION_CHANGED;
                    description = firstItem.getDescription();
                    invoice.voidInvoice();
                    log.error("invoice.payment_succeeded & subscription_update: invoice is voided for CustId: {}", invoice.getCustomer());
                  }
                } else {
                  log.error("invoice.payment_succeeded & subscription_update: failed to find account by CustId: {}", invoice.getCustomer());
                }
                break;
              }

            }

            //accountId must not be null for both a newcomer or a renewal!
            if (accountId != null) {
              AccountTrans trans = new AccountTrans();
              trans.setAccountId(accountId);
              trans.setEventId(event.getId());
              trans.setEvent(subsEvent);
              trans.setSuccessful(successful);
              trans.setReason(invoice.getBillingReason());
              trans.setDescription(description);
              trans.setFileUrl(invoice.getHostedInvoiceUrl());
              res = addhandle(accountId, invoice.getCustomer(), dto, trans);
            } else {
              log.error("Failed to set accountId, see the previous log rows!");
              res = Responses.DataProblem.SUBSCRIPTION_PROBLEM;
            }

          } catch (Exception e) {
            log.error("Failed to handle invoice.payment_succeeded event.", e);
            res = Responses.ServerProblem.EXCEPTION;
          }

          break;
        }

        case "invoice.payment_failed": {
          Invoice invoice = (Invoice) stripeObject;
          Long accountId = null;

          //the checkout must be completed
          try (Handle handle = Database.getHandle()) {
            Charge charge = Charge.retrieve(invoice.getCharge());
            if (charge != null) {
              InvoiceLineItem li = (
                invoice.getLines() != null && 
                invoice.getLines().getData() != null && 
                invoice.getLines().getData().size() > 0 
                ? invoice.getLines().getData().get(0) 
                : null
              );

              //if it is a first payment of a new subscriber
              if (li != null) {
                CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
                String checkoutHash = li.getMetadata().get("hash");
                if (StringUtils.isNotBlank(checkoutHash)) {
                  Checkout checkout = checkoutDao.findByHash(checkoutHash);
                  if (checkout != null) {
                    checkoutDao.update(checkoutHash, CheckoutStatus.FAILED.name(), charge.getFailureMessage());
                  } else {
                    log.error("Failed to complete the checkout! Hash: {}", checkoutHash);
                  }
                } else {
                  log.error("Failed to get checkout! Hash is null.");
                }
              } else { //or an existing subscriber
                if (accountId == null) {
                  AccountDao accountDao = handle.attach(AccountDao.class);
                  Account account = accountDao.findByCustId(invoice.getCustomer());
                  if (account != null) {
                    accountId = account.getId();
                  } else {
                    log.error("invoice.payment_failed: failed to find account by CustId: {}", invoice.getCustomer());
                  }
                }
              }
            }
            
          } catch (Exception e) {
            accountId = null;
            log.error("Failed to handle invoice.payment_failed event", e);
          }
          
          //accountId must not be null!
          if (accountId != null) {
            AccountTrans trans = new AccountTrans();
            trans.setEventId(event.getId());
            trans.setEvent(SubsEvent.PAYMENT_FAILED);
            trans.setSuccessful(Boolean.FALSE);
            trans.setReason(invoice.getBillingReason());
            trans.setFileUrl(invoice.getHostedInvoiceUrl());
            res = addhandle(accountId, invoice.getCustomer(), null, trans);
          } else {
            log.error("User's credit card failed!");
            res = Responses.ServerProblem.CHECKOUT_PROBLEM;
          }
          break;
        }

        default: {
          log.warn("Stripe event -{}- happened!", event.getType());
          res = Responses.OK;
          break;
        }
      }
    } else {
      log.error("Failed to parse stripe event object. Type: " + event.getType());
      res = new Response("Failed to parse stripe event object!");
    }
 
    return res;
  }

  Response addhandle(Long account_id, String custId, CustomerDTO dto, AccountTrans trans) {
    Response response = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    try (Handle handle = Database.getHandle()) {
    	handle.begin();

      AccountDao accountDao = handle.attach(AccountDao.class);
      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
      AccountTrans oldTrans = subscriptionDao.findByEventId(trans.getEventId());

      if (oldTrans == null) {
        Long accountId = account_id;
        if (accountId == null && trans != null && trans.getAccountId() != null) accountId = trans.getAccountId();

        Account account = null;
        if (accountId != null) {
          account = accountDao.findById(accountId);
        } else if (StringUtils.isNotBlank(custId)) {
          account = accountDao.findByCustId(custId);
        }
  
        if (account != null) {

          if (accountId == null) accountId = account.getId();
          if (trans.getAccountId() == null) trans.setAccountId(accountId);
    
          switch (trans.getEvent()) {
    
            case SUBSCRIPTION_STARTED: {
              if (account.getStatus().isOKForSubscription()) {

                Plan plan = Plans.findByName(dto.getPlanName());
                if (subscriptionDao.startSubscription(dto, AccountStatus.SUBSCRIBED.name(), plan.getLinkLimit(), accountId)) {
                  boolean isOK = updateInvoiceInfo(dto);
                  if (isOK) {

                    String accountName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : account.getName();

                    Map<String, Object> dataMap = new HashMap<>(5);
                    dataMap.put("user", dto.getEmail());
                    dataMap.put("account", accountName);
                    dataMap.put("plan", dto.getPlanName());
                    dataMap.put("invoiceUrl", trans.getFileUrl());
                    dataMap.put("renewalAt", DateUtils.formatReverseDate(dto.getRenewalDate()));
                    String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_STARTED, dataMap);
                    emailSender.send(Props.APP_EMAIL_SENDER(), "Your subscription for inprice just started", dto.getEmail(), message);

                    response = Responses.OK;
                    log.info("A new subscription is started: Title: {}, Account Id: {}", dto.getTitle(), accountId);
                  } else {
                    log.warn("Stripe service error: Title: {}, Account Id: {}", dto.getTitle(), accountId);
                  }
                } else {
                  log.warn("Failed to start a new subscription: Title: {}, Account Id: {}", dto.getTitle(), accountId);
                }
              } else {
                response = Responses.Already.ACTIVE_SUBSCRIPTION;
              }
              break;
            }
    
            case SUBSCRIPTION_RENEWED: {
              if (subscriptionDao.renewSubscription(accountId, AccountStatus.SUBSCRIBED.name(), dto.getRenewalDate())) {

                String accountName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : account.getName();

                Map<String, Object> dataMap = new HashMap<>(5);
                dataMap.put("user", dto.getEmail());
                dataMap.put("account", accountName);
                dataMap.put("plan", dto.getPlanName());
                dataMap.put("invoiceUrl", trans.getFileUrl());
                dataMap.put("renewalAt", DateUtils.formatReverseDate(dto.getRenewalDate()));
                String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_RENEWAL, dataMap);
                emailSender.send(Props.APP_EMAIL_SENDER(), "Your subscription is extended", dto.getEmail(), message);

                response = Responses.OK;
                log.info("Subscription is renewed: Account Id: {}", accountId);
              } else {
                log.warn("Failed to renew a subscription: Account Id: {}", accountId);
              }
              break;
            }
    
            case SUBSCRIPTION_CANCELLED: {
              if (account.getStatus().isOKForCancel()) {
                Date now = new Date();
                long days = DateUtils.findDayDiff(now, account.getRenewalAt());
                
                boolean isOK = false;
                String couponCode = null;

                if (days > 3) {
                  couponCode = 
                    couponService.createCoupon(
                      handle, 
                      account.getId(), 
                      trans.getEvent(), 
                      account.getPlanName(), 
                      days, 
                      "For "+days+" days remaining cancelation"
                    );
                }

                isOK = subscriptionDao.terminate(accountId, AccountStatus.CANCELLED.name());

                if (isOK) {
                  String accountName = StringUtils.isNotBlank(account.getTitle()) ? account.getTitle() : account.getName();

                  Map<String, Object> mailMap = new HashMap<>(5);
                  mailMap.put("account", accountName);
                  mailMap.put("user", CurrentUser.getEmail());
                  if (couponCode != null) {
                    mailMap.put("couponCode", couponCode);
                    mailMap.put("days", days);
                    mailMap.put("planName", account.getPlanName());
                  }
                  String message = 
                    templateRenderer.render(
                      (
                        days < 4
                        ? EmailTemplate.SUBSCRIPTION_CANCELLED
                        : EmailTemplate.SUBSCRIPTION_CANCELLED_COUPONED
                      ), mailMap
                  );
                  emailSender.send(
                    Props.APP_EMAIL_SENDER(), 
                    "Notification about your cancelled plan.", CurrentUser.getEmail(), 
                    message
                  );

                  response = Responses.OK;
                  log.info("Subscription cancelled: Account: {}, Pre.Status: {}", accountId, account.getStatus());
                } else {
                  log.warn("Failed to cancel subscription: Account: {}, Status: {}", accountId, account.getStatus());
                }
              } else {
                response = Responses.Already.PASSIVE_SUBSCRIPTION;
              }
              break;
            }
    
            case PAYMENT_FAILED: {
              EmailTemplate template = null;

              String accountName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : account.getName();

              Map<String, Object> dataMap = new HashMap<>(4);
              dataMap.put("user", dto.getEmail());
              dataMap.put("account", accountName);
              if (dto.getRenewalDate() != null) {
                long days = 3 + DateUtils.findDayDiff(new Date(), dto.getRenewalDate());
                if (days > 0) {
                  Date lastTryingDate = org.apache.commons.lang3.time.DateUtils.addDays(dto.getRenewalDate(), 3);
                  dataMap.put("days", days);
                  dataMap.put("lastTryingDate", DateUtils.formatReverseDate(lastTryingDate));
                  template = EmailTemplate.PAYMENT_FAILED_HAS_MORE_DAYS;
                } else {
                  template = EmailTemplate.PAYMENT_FAILED_LAST_TIME;
                }
              } else {
                template = EmailTemplate.PAYMENT_FAILED_FIRST_TIME;
              }
              String message = templateRenderer.render(template, dataMap);
              emailSender.send(Props.APP_EMAIL_SENDER(), "Your payment failed", dto.getEmail(), message);

              response = Responses.OK; // must be set true so that the handle can reflect on database
              log.warn("Payment failed! Account Id: {}", accountId);
              break;
            }
    
            case SUBSCRIPTION_CHANGED: {
              Plan newPlan = Plans.findByName(dto.getPlanName());
              boolean isOK = subscriptionDao.changePlan(accountId, newPlan.getName(), newPlan.getLinkLimit());
              if (isOK) {

                //if it is a downgrade an there are more than three days to renewal date
                //user is given a coupon to compansate those days
                String couponCode = null;
                long days = 0;
                Plan oldPlan = Plans.findByName(account.getPlanName());

                if (newPlan.getId().intValue() < oldPlan.getId().intValue()) {
                  days = DateUtils.findDayDiff(new Date(), account.getRenewalAt());
                  if (days > 3) {
                    couponCode = 
                      couponService.createCoupon(
                        handle, 
                        account.getId(), 
                        trans.getEvent(), 
                        oldPlan.getName(), 
                        days, 
                        "For "+days+" days remaining after downgrade operation"
                      );
                  }
                }

                String accountName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : account.getName();

                Map<String, Object> dataMap = new HashMap<>(7);
                dataMap.put("user", dto.getEmail());
                dataMap.put("account", accountName);
                dataMap.put("fromPlan", account.getPlanName());
                dataMap.put("toPlan", newPlan.getName());
                dataMap.put("invoiceUrl", trans.getFileUrl());
                dataMap.put("days", days);
                dataMap.put("couponCode", couponCode);
                String message = templateRenderer.render(
                  (
                    trans.getSuccessful() 
                    ? (couponCode != null ? EmailTemplate.SUBSCRIPTION_CHANGE_SUCCESSFUL_COUPONED : EmailTemplate.SUBSCRIPTION_CHANGE_SUCCESSFUL)
                    : EmailTemplate.SUBSCRIPTION_CHANGE_FAILED
                  ), dataMap
                );
                emailSender.send(Props.APP_EMAIL_SENDER(), "Your account plan has changed to " + newPlan.getName(), dto.getEmail(), message);
                
                response = Responses.OK;
                log.info("Subscription is changed to {}. Account: {}", newPlan.getName(), accountId);

              } else {
                log.error("Failed to change plan! Account: {}, Plan: {}", account.getId(), newPlan.getName());
              }

              break;
            }

            default:
              log.warn("Unexpected event! Account Id: {}, Event: {}", accountId, trans.getEvent().name());
              break;
    
          }

          if (response.isOK()) {
            boolean isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());

            if (isOK) {
              switch (trans.getEvent()) {

                case SUBSCRIPTION_STARTED:
                case SUBSCRIPTION_CHANGED: {
                  isOK = 
                    accountDao.insertStatusHistory(
                      account.getId(), 
                      AccountStatus.SUBSCRIBED.name(),
                      dto.getPlanName(),
                      dto.getSubsId(),
                      dto.getCustId()
                    );
                  break;
                }

                case SUBSCRIPTION_CANCELLED: {
                  isOK = accountDao.insertStatusHistory(account.getId(), AccountStatus.CANCELLED.name());
                  break;
                }

                default:
                  break;
              }
            }

            if (! isOK) {
              response = Responses.DataProblem.DB_PROBLEM;
            }
          } 

        } else {
          log.warn("Account not found! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
          response = Responses.NotFound.ACCOUNT;
        }
  
      } else {
        log.warn("Idompotent event! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
        response = Responses.DataProblem.ALREADY_EXISTS;
      }

      if (response.isOK())
      	handle.commit();
      else
      	handle.rollback();
    }

    return response;
  }

  private CustomerDTO generateCustDTO(Invoice invoice) {
    CustomerDTO dto = new CustomerDTO();
    dto.setEmail(invoice.getCustomerEmail());

    Address address = null;
    if (StringUtils.isNotBlank(invoice.getCharge())) {
      try {
        Charge charge = Charge.retrieve(invoice.getCharge());
        address = charge.getBillingDetails().getAddress();
        dto.setTitle(charge.getBillingDetails().getName());
      } catch (StripeException e) {
        log.warn("Problem with getting charge from the invoce: " + invoice.getId(), e);
      }
    }

    if (address == null) {
      address = invoice.getCustomerAddress();
      dto.setTitle(invoice.getCustomerName());
      log.warn("Invoce: {} is free of charge!", invoice.getId());
    }

    dto.setAddress1(address.getLine1());
    dto.setAddress2(address.getLine2());
    dto.setPostcode(address.getPostalCode());
    dto.setCity(address.getCity());
    dto.setState(address.getState());
    dto.setCountry(address.getCountry());
    dto.setCustId(invoice.getCustomer());
    dto.setSubsId(invoice.getSubscription());

    return dto;
  }

  private boolean updateInvoiceInfo(CustomerDTO dto) {
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
        .setEmail(dto.getEmail())
        .setAddress(address)
      .build();

    try {
      Customer customer = Customer.retrieve(dto.getCustId()).update(customerParams);
      log.info("Customer info is updated, Subs Customer Id: {}, Title: {}, Email: {}", customer.getId(), dto.getTitle(), dto.getEmail());
      return (customer != null);
    } catch (StripeException e) {
      log.error("Failed to update a new customer in Stripe", e);
    }
    return false;
  }

}