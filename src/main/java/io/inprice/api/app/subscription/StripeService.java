package io.inprice.api.app.subscription;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

import io.inprice.api.app.company.CompanyDao;
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
import io.inprice.common.meta.CheckoutStatus;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Checkout;
import io.inprice.common.models.Company;
import io.inprice.common.models.CompanyTrans;
import io.inprice.common.models.Plan;
import io.inprice.common.utils.CouponManager;
import io.inprice.common.utils.DateUtils;

class StripeService {

  private static final Logger log = LoggerFactory.getLogger(StripeService.class);

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
      String subsCustomerId = null;

      try (Handle handle = Database.getHandle()) {
        CompanyDao companyDao = handle.attach(CompanyDao.class);
        Company company = companyDao.findById(CurrentUser.getCompanyId());

        //check if the last checkout is active
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        Checkout checkout = checkoutDao.findLastCheckout(CurrentUser.getCompanyId());
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
          } else if (CheckoutStatus.SUCCESSFUL.equals(checkout.getStatus()) && CompanyStatus.SUBSCRIBED.equals(company.getStatus())) {
            message = "Your subscription and (last) checkout are successful. You cannot create a new checkout!";
          }
          if (message != null) return new Response(message);
        }

        //finding Customer Id if exists previously
        subsCustomerId = company.getSubsCustomerId();

        // only broader plan transitions allowed
        if (company.getProductCount().compareTo(plan.getProductLimit()) > 0) {
          return Responses.PermissionProblem.BROADER_PLAN_NEEDED;
        }
      }

      SessionCreateParams params = SessionCreateParams.builder()
        .setCustomer(subsCustomerId)
        .setCustomerEmail(StringUtils.isBlank(subsCustomerId) ? CurrentUser.getEmail() : null)
        .setClientReferenceId(CurrentUser.getCompanyId().toString())
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
        boolean isOK = checkoutDao.insert(checkoutHash, session.getId(), CurrentUser.getCompanyId(), plan.getName());
        if (isOK) {
          return new Response(Collections.singletonMap("sessionId", session.getId()));
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
          if (checkoutDao.update(checkoutHash, CheckoutStatus.CANCELLED.name(), "Cancelled by user.")) {
            return Responses.OK;
          } else {
            log.error("Failed to update checkout! Hash: {}", checkoutHash);
          }
        } else {
          log.error("Failed to cancel checkout! Hash: {}", checkoutHash);
        }
      } else {
        log.error("Failed to get checkout info! Hash is null.");
      }
    }
    return new Response("Failed to find checkout!");
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
  Response cancel(Handle handle, Company company) {
    boolean isAlreadyCancelled = false;
    Subscription subsResult = null;
    try {
      Subscription subscription = Subscription.retrieve(company.getSubsId());
      subsResult = subscription.cancel();
    } catch (InvalidRequestException e) {
      isAlreadyCancelled = e.getMessage().indexOf("No such subscription") > -1;
    } catch (Exception e) {
      log.error("Failed to cancel subs", e);
    }

    String couponCode = null;

    // if a subscriber cancels while he/she has remaining days to renewal, he/she will be given a coupon to amotise
    if (isAlreadyCancelled || subsResult != null) {
      if (isAlreadyCancelled || subsResult.getStatus().equals("canceled")) {
        Date now = new Date();
        long days = DateUtils.findDayDiff(now, company.getSubsRenewalAt());
        if (isAlreadyCancelled == false && days > 3) {
          CouponDao couponDao = handle.attach(CouponDao.class);
          couponCode = CouponManager.generate();
          boolean isOK = couponDao.create(
            couponCode,
            company.getPlanName(),
            days,
            String.format(
              "In exchange for %d usage remaining cancelation at %s",
              days, DateUtils.formatReverseDate(now)
            )
          );
          if (!isOK) couponCode = null;
        }
        Map<String, String> dataMap = new HashMap<>(3);
        dataMap.put("couponCode", couponCode);
        if (couponCode != null) {
          dataMap.put("days", ""+days);
          dataMap.put("planName", company.getPlanName());
        }
        log.info("{} cancelled!", company.getName());
        return new Response(dataMap);
      } else if (subsResult != null) {
        log.error("Unexpected subs status: {}", subsResult.getStatus());
      }
    } else {
      log.error("Not a cancelled subscription and subsResult is null!");
    }
    log.error("Subs cancellation problem for Company: {}", company.getId());
    return Responses.DataProblem.SUBSCRIPTION_PROBLEM;
  }

  /**
   * Upgrades or Downgrads the plan
   * 
   */
  Response changePlan(int newPlanId) {
    Response res = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    Company company = null;

    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      company = companyDao.findById(CurrentUser.getCompanyId());
    }

    if (company != null) {
      if (CompanyStatus.SUBSCRIBED.equals(company.getStatus())) {
        if (StringUtils.isNotBlank(company.getSubsId())) {

          Plan newPlan = Plans.findById(newPlanId);
          if (newPlan != null) {

            if (! newPlan.getName().equals(company.getPlanName())) {
              boolean noNeedABroaderPlan =
                (company.getProductCount() == 0) || (company.getProductCount().compareTo(newPlan.getProductLimit()) <= 0);

              if (noNeedABroaderPlan) {
                try {
                  Subscription subscription = Subscription.retrieve(company.getSubsId());

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
                  
                  EmailTemplate emailTemplate = null;
                  Subscription subs = subscription.update(params);
                  Invoice invoice = Invoice.retrieve(subs.getLatestInvoice());

                  Map<String, Object> dataMap = new HashMap<>(5);
                  dataMap.put("user", invoice.getCustomerEmail());
                  dataMap.put("fromPlan", company.getPlanName());
                  dataMap.put("toPlan", newPlan.getName());

                  if (subs.getPendingUpdate() != null) {
                    emailTemplate = EmailTemplate.SUBSCRIPTION_CHANGE_FAILED;
                    invoice.voidInvoice();
                    res = Responses.NotSuitable.PAYMENT_FAILURE_ON_PLAN_CHANGE;
                  } else {
                    emailTemplate = EmailTemplate.SUBSCRIPTION_CHANGE_SUCCESSFUL;
                    dataMap.put("invoiceUrl", invoice.getHostedInvoiceUrl());
                    company.setPlanName(newPlan.getName());
                    company.setProductLimit(newPlan.getProductLimit());
                    res = Commons.refreshSession(company);
                  }
                  String message = templateRenderer.render(emailTemplate, dataMap);
                  emailSender.send(Props.APP_EMAIL_SENDER(), "Your plan transition", invoice.getCustomerEmail(), message);

                } catch (Exception e) {
                  log.error("Failed to change plan. Company: " + company.getId() + ", New Plan: " + newPlanId, e);
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
      res = Responses.NotFound.COMPANY;
    }

    return res;
  }

  /**
   * Handles failed and successful invoice transactions to manage subscriptions
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

          try {
            InvoiceLineItem li = invoice.getLines().getData().get(0);

            Long companyId = null;
            SubsEvent subsEvent = null;

            CustomerDTO dto = new CustomerDTO();
            dto.setRenewalDate(new java.sql.Timestamp(li.getPeriod().getEnd() * 1000));
            dto.setEmail(invoice.getCustomerEmail());

            switch (invoice.getBillingReason()) {

              //a new subscriber
              case "subscription_create": {
                String title = null;
                Address address = null;
  
                if (StringUtils.isNotBlank(invoice.getCharge())) {
                  Charge charge = Charge.retrieve(invoice.getCharge());
                  if (charge != null) {
                    address = charge.getBillingDetails().getAddress();
                    title = charge.getBillingDetails().getName();
                  }
                }
  
                if (address == null) {
                  address = invoice.getCustomerAddress();
                  title = invoice.getCustomerName();
                  log.warn("Invoce: {} is free of charge!", invoice.getId());
                }
  
                dto.setTitle(title);
                dto.setAddress1(address.getLine1());
                dto.setAddress2(address.getLine2());
                dto.setPostcode(address.getPostalCode());
                dto.setCity(address.getCity());
                dto.setState(address.getState());
                dto.setCountry(address.getCountry());
                dto.setCustId(invoice.getCustomer());
                dto.setSubsId(invoice.getSubscription());
  
                //the checkout must be completed
                try (Handle handle = Database.getHandle()) {
                  CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
                  String checkoutHash = li.getMetadata().get("hash");
                  if (StringUtils.isNotBlank(checkoutHash)) {
                    Checkout checkout = checkoutDao.findByHash(checkoutHash);
                    if (checkout != null) {
                      if (checkoutDao.update(checkoutHash, CheckoutStatus.SUCCESSFUL.name(), null)) {
                        companyId = checkout.getCompanyId();
                        dto.setPlanName(checkout.getPlanName());
                        subsEvent = SubsEvent.SUBSCRIPTION_STARTED;
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
                  CompanyDao companyDao = handle.attach(CompanyDao.class);
                  Company company = companyDao.findBySubsCustomerId(invoice.getCustomer());
                  if (company != null) {
                    companyId = company.getId();
                    subsEvent = SubsEvent.SUBSCRIPTION_RENEWED;
                  } else {
                    log.error("invoice.payment_succeeded & subscription_cycle: failed to find company by SubsCustomerId: {}", invoice.getCustomer());
                  }
                }
                break;
              }

              //plan changing
              case "subscription_update": {
                if (invoice.getStatus().equals("paid")) {
                  Plan newPlan = Plans.findByPriceId(li.getPrice().getId());
                  if (newPlan != null) {
                    try (Handle handle = Database.getHandle()) {
                      CompanyDao companyDao = handle.attach(CompanyDao.class);
                      Company company = companyDao.findBySubsCustomerId(invoice.getCustomer());
                      if (company != null) {
                        companyId = company.getId();
                        subsEvent = SubsEvent.SUBSCRIPTION_CHANGED;

                        boolean isOK = companyDao.changePlan(companyId, newPlan.getName(), newPlan.getProductLimit());
                        if (! isOK) {
                          companyId = null;
                          log.error("Failed to change plan! Company: {}, Plan: {}", company.getId(), newPlan.getName());
                        }
                      } else {
                        log.error("invoice.payment_succeeded & subscription_update: failed to find company by SubsCustomerId: {}, Plan: {}", 
                          invoice.getCustomer(), li.getPrice().getId());
                      }
                    }
                  } else {
                    log.error("invoice.payment_succeeded & subscription_update: failed to find plan for SubsCustomerId: {}, Plan: {}", 
                      invoice.getCustomer(), li.getPrice().getId());
                  }
                } else {
                  invoice.voidInvoice();
                  log.error("invoice.payment_succeeded & subscription_update: invoice is voided for SubsCustomerId: {}, Plan: {}", 
                      invoice.getCustomer(), li.getPrice().getId());
                }
                break;
              }

            }

            //companyId must not be null for both a newcomer or a renewal!
            if (companyId != null) {
              CompanyTrans trans = new CompanyTrans();
              trans.setCompanyId(companyId);
              trans.setEventId(event.getId());
              trans.setEvent(subsEvent);
              trans.setSuccessful(Boolean.TRUE);
              trans.setReason(invoice.getBillingReason());
              trans.setDescription(li.getDescription());
              trans.setFileUrl(invoice.getHostedInvoiceUrl());
              res = addTransaction(null, invoice.getCustomer(), dto, trans);
            } else {
              log.error("Failed to set companyId, see the previous log rows!");
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
          Long companyId = null;

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
                    if (checkoutDao.update(checkoutHash, CheckoutStatus.FAILED.name(), charge.getFailureMessage())) {
                      companyId = checkout.getCompanyId();
                    } else {
                      log.error("Failed to update checkout! Hash: {}", checkoutHash);
                    }
                  } else {
                    log.error("Failed to complete the checkout! Hash: {}", checkoutHash);
                  }
                } else {
                  log.error("Failed to get checkout! Hash is null.");
                }
              } else { //or an existing subscriber
                if (companyId == null) {
                  CompanyDao companyDao = handle.attach(CompanyDao.class);
                  Company company = companyDao.findBySubsCustomerId(invoice.getCustomer());
                  if (company != null) {
                    companyId = company.getId();
                  } else {
                    log.error("invoice.payment_failed: failed to find company by SubsCustomerId: {}", invoice.getCustomer());
                  }
                }
              }
            }
            
          } catch (Exception e) {
            companyId = null;
            log.error("Failed to handle invoice.payment_failed event", e);
          }
          
          //companyId must not be null!
          if (companyId != null) {
            CompanyTrans trans = new CompanyTrans();
            trans.setEventId(event.getId());
            trans.setEvent(SubsEvent.PAYMENT_FAILED);
            trans.setSuccessful(Boolean.FALSE);
            trans.setReason(invoice.getBillingReason());
            trans.setFileUrl(invoice.getHostedInvoiceUrl());
            res = addTransaction(companyId, invoice.getCustomer(), null, trans);
          } else {
            log.error("Failed to set companyId!");
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

  Response addTransaction(Long company_id, String subsCustomerId, CustomerDTO dto, CompanyTrans trans) {
    Response[] res = { Responses.DataProblem.SUBSCRIPTION_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {
        CompanyDao companyDao = transactional.attach(CompanyDao.class);
        SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

        CompanyTrans oldTrans = subscriptionDao.findByEventId(trans.getEventId());
        if (oldTrans == null) {

          Long companyId = company_id;
          if (companyId == null && trans != null && trans.getCompanyId() != null) companyId = trans.getCompanyId();

          Company company = null;
          if (companyId != null) {
            company = companyDao.findById(companyId);
          } else if (StringUtils.isNotBlank(subsCustomerId)) {
            company = companyDao.findBySubsCustomerId(subsCustomerId);
          }
    
          if (company != null) {

            if (companyId == null) companyId = company.getId();
            if (trans.getCompanyId() == null) trans.setCompanyId(companyId);
      
            switch (trans.getEvent()) {
      
              case SUBSCRIPTION_STARTED: {
                if (company.getStatus().isOKForSubscription()) {

                  if (companyDao.startSubscription(dto, CompanyStatus.SUBSCRIBED.name(), companyId)) {
                    boolean isOK = updateInvoiceInfo(dto);
                    if (isOK) {

                      String companyName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : company.getName();

                      Map<String, Object> dataMap = new HashMap<>(5);
                      dataMap.put("user", dto.getEmail());
                      dataMap.put("company", companyName);
                      dataMap.put("plan", dto.getPlanName());
                      dataMap.put("invoiceUrl", trans.getFileUrl());
                      dataMap.put("subsRenewalAt", DateUtils.formatReverseDate(dto.getRenewalDate()));
                      String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_STARTED, dataMap);
                      emailSender.send(Props.APP_EMAIL_SENDER(), "Your subscription for inprice just started", dto.getEmail(), message);

                      res[0] = Responses.OK;
                      log.info("A new subscription is started: Title: {}, Company Id: {}", dto.getTitle(), companyId);
                    } else {
                      log.warn("Stripe service error: Title: {}, Company Id: {}", dto.getTitle(), companyId);
                    }
                  } else {
                    log.warn("Failed to start a new subscription: Title: {}, Company Id: {}", dto.getTitle(), companyId);
                  }
                } else {
                  res[0] = Responses.Already.ACTIVE_SUBSCRIPTION;
                }
                break;
              }
      
              case SUBSCRIPTION_RENEWED: {
                if (companyDao.renewSubscription(companyId, CompanyStatus.SUBSCRIBED.name(), dto.getRenewalDate())) {

                  String companyName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : company.getName();

                  Map<String, Object> dataMap = new HashMap<>(5);
                  dataMap.put("user", dto.getEmail());
                  dataMap.put("company", companyName);
                  dataMap.put("plan", dto.getPlanName());
                  dataMap.put("invoiceUrl", trans.getFileUrl());
                  dataMap.put("subsRenewalAt", DateUtils.formatReverseDate(dto.getRenewalDate()));
                  String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_RENEWAL, dataMap);
                  emailSender.send(Props.APP_EMAIL_SENDER(), "Your subscription is extended", dto.getEmail(), message);

                  res[0] = Responses.OK;
                  log.info("Subscription is renewed: Company Id: {}", companyId);
                } else {
                  log.warn("Failed to renew a subscription: Company Id: {}", companyId);
                }
                break;
              }
      
              case SUBSCRIPTION_CANCELLED: {
                if (company.getStatus().isOKForCancel()) {
                  if (companyDao.cancelSubscription(companyId)) {

                    String companyName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : company.getName();

                    Map<String, Object> dataMap = new HashMap<>(5);
                    dataMap.put("user", dto.getEmail());
                    dataMap.put("company", companyName);
                    String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_CANCELLED, dataMap);
                    emailSender.send(Props.APP_EMAIL_SENDER(), "inprice subscription is cancelled", dto.getEmail(), message);
  
                    res[0] = Responses.OK;
                    log.info("Subscription is cancelled: Company: {}, Pre.Status: {}", companyId, company.getStatus());
                  } else {
                    log.warn("Failed to cancel a subscription: Company: {}, Status: {}", companyId, company.getStatus());
                  }

                } else {
                  res[0] = Responses.Already.PASSIVE_SUBSCRIPTION;
                }
                break;
              }
      
              case PAYMENT_FAILED: {
                EmailTemplate template = null;

                String companyName = StringUtils.isNotBlank(dto.getTitle()) ? dto.getTitle() : company.getName();

                Map<String, Object> dataMap = new HashMap<>(4);
                dataMap.put("user", dto.getEmail());
                dataMap.put("company", companyName);
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

                res[0] = Responses.OK; // must be set true so that the transactional can reflect on database
                log.warn("Payment failed! Company Id: {}", companyId);
                break;
              }

              default:
                log.warn("Unexpected event! Company Id: {}, Event: {}", companyId, trans.getEvent().name());
                break;
      
            }

            if (res[0].isOK()) {
              boolean isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                if (trans.getEvent().equals(SubsEvent.SUBSCRIPTION_STARTED)) {
                  isOK = 
                    companyDao.insertStatusHistory(
                      company.getId(), 
                      CompanyStatus.SUBSCRIBED.name(),
                      dto.getPlanName(),
                      dto.getSubsId(),
                      dto.getCustId()
                    );
                } else if (trans.getEvent().equals(SubsEvent.SUBSCRIPTION_CANCELLED)) {
                  isOK = companyDao.insertStatusHistory(company.getId(), CompanyStatus.CANCELLED.name());
                }
              }

              if (! isOK) {
                res[0] = Responses.DataProblem.DB_PROBLEM;
              }
            } 

          } else {
            log.warn("Company not found! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
            res[0] = Responses.NotFound.COMPANY;
          }
    
        } else {
          log.warn("Idompotent event! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
          res[0] = Responses.DataProblem.ALREADY_EXISTS;
        }
  
        return res[0].isOK();
      });
    }

    return res[0];
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