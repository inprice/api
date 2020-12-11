package io.inprice.api.app.subscription;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import io.inprice.common.models.CompanyHistory;
import io.inprice.common.models.CompanyTrans;
import io.inprice.common.models.Plan;
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
  Response createCheckoutSession(int planId) {
    Plan plan = Plans.findById(planId);

    if (plan != null) {

      String checkoutHash = CodeGenerator.hash();
      String subsCustomerId = null;
      long trialPeriodDays = 0;

      try (Handle handle = Database.getHandle()) {
        CompanyDao companyDao = handle.attach(CompanyDao.class);
        Company company = companyDao.findById(CurrentUser.getCompanyId());

        //check if the last checkout is active
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        Checkout checkout = checkoutDao.findLastCheckout(CurrentUser.getCompanyId());
        if (checkout != null) {
          if (CheckoutStatus.PENDING.equals(checkout.getStatus())) {
            long diff = DateUtils.findHourDiff(checkout.getCreatedAt(), new Date());
            String delay = (diff > 0 ? diff + " hour(s) more" : " a couple of minutes");
            return new Response("You have an active checkout. Please wait " + delay + " for it to complete.");
          } else if (CheckoutStatus.SUCCESSFUL.equals(checkout.getStatus()) && CompanyStatus.SUBSCRIBED.equals(company.getStatus())) {
            return new Response("Your subscription and (last) checkout are successful. You cannot create a new checkout!");
          }
        }

        //finding Customer Id if exists previously
        subsCustomerId = company.getSubsCustomerId();

        // only broader plan transitions allowed
        if (company.getProductCount().compareTo(plan.getProductLimit()) <= 0) {

          // if company's current status equals to CANCELLED and previous status equals to SUBSCRIBED 
          // and also has more than 0 days. the remaining days are added up to new renewal date
          if (company != null && CompanyStatus.CANCELLED.equals(company.getStatus())) {

            SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
            CompanyHistory prevHistory = subscriptionDao.findPreviousHistoryRowByStatusAndCompanyId(CurrentUser.getCompanyId(), CompanyStatus.SUBSCRIBED.name());

            if (prevHistory != null) {
              CompanyStatus prevStatus = prevHistory.getStatus();
              if (CompanyStatus.SUBSCRIBED.equals(prevStatus)) {
                long diff = DateUtils.findDayDiff(new Date(), company.getSubsRenewalAt());
                if (diff > 0) {
                  trialPeriodDays = diff;
                }
              }
            }
          }
        } else {
          return Responses.PermissionProblem.PLAN_TRANSITION_PROBLEM;
        }
      }

      // used for adding session data (especially trialPeriodDays for upgraders and downgraders)
      SessionCreateParams.SubscriptionData.Builder scpBuilder = 
        SessionCreateParams.SubscriptionData.builder().putMetadata("hash", checkoutHash);

      if (trialPeriodDays > 0) {
        scpBuilder = scpBuilder.setTrialPeriodDays(trialPeriodDays);
      }

      SessionCreateParams params = SessionCreateParams.builder()
        .setCustomer(subsCustomerId)
        .setCustomerEmail(CurrentUser.getEmail())
        .setClientReferenceId(CurrentUser.getCompanyId().toString())
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
        .setSuccessUrl(Props.APP_WEB_URL() + "/payment-ok?hash="+checkoutHash)
        .setCancelUrl(Props.APP_WEB_URL() + "/payment-cancel?hash="+checkoutHash)
        .setSubscriptionData(scpBuilder.build())
        .putMetadata("description", plan.getFeatures().get(0))
        .addLineItem(
          SessionCreateParams.LineItem
            .builder()  
              .setQuantity(1L)
              .setPrice(plan.getStripePriceId()
            ).build()
          )
        .build();

      try (Handle handle = Database.getHandle()) {
        Session session = Session.create(params);

        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        boolean isOK = checkoutDao.insert(checkoutHash, session.getId(), CurrentUser.getCompanyId(), plan.getName());
        if (isOK) {
          return new Response(Collections.singletonMap("sessionId", session.getId()));
        } else {
          return Responses.ServerProblem.CHECKOUT_PROBLEM;
        }

      } catch (StripeException e) {
        log.error("Failed to create checkout session", e);
        return Responses.ServerProblem.CHECKOUT_PROBLEM;
      }
    }

    return Responses.NotFound.PLAN;
  }

  /**
   * Cancel operations should be taken into account immediately.
   * Instead of waiting cancel hook in handleHookEvent, we manage cancelation things in this method.
   * 
   * @param company
   */
  Response cancel(Company company) {
    try {
      Subscription subscription = Subscription.retrieve(company.getSubsId());
      Subscription subsResult = subscription.cancel();
      if (subsResult != null && subsResult.getStatus().equals("canceled")) {
        log.info("{} is cancelled subscription!", company.getName());
        return new Response(subsResult.getId());
      } else if (subsResult != null) {
        log.warn("Unexpected subs status: {}", subsResult.getStatus());
      } else {
        log.error("subsResult is null!");
      }
    } catch (Exception e) {
      log.error("Failed to cancel subs", e);
    }
    log.error("Subs cancellation problem for Company: {}", company.getId());
    return Responses.DataProblem.SUBSCRIPTION_PROBLEM;
  }

  Response cancelCheckout(String checkoutHash) {
    try (Handle handle = Database.getHandle()) {
      if (StringUtils.isNotBlank(checkoutHash)) {
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);
        Checkout checkout = checkoutDao.updateByHash(checkoutHash, CheckoutStatus.CANCELLED.name(), "Cancelled by user.");
        if (checkout != null) {
          return Responses.OK;
        } else {
          log.error("Failed to cancel checkout! Hash: {}", checkoutHash);
        }
      } else {
        log.error("Failed to get checkout info! Hash is null.");
      }
    }
    return new Response("Failed to find the checkout!");
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
            Charge charge = Charge.retrieve(invoice.getCharge());
            if (charge != null) {
              InvoiceLineItem li = invoice.getLines().getData().get(0);

              Long companyId = null;
              SubsEvent subsEvent = null;

              CustomerDTO dto = new CustomerDTO();
              dto.setRenewalDate(new java.sql.Timestamp(li.getPeriod().getEnd() * 1000));

              //a new subscriber
              if (invoice.getBillingReason().equals("subscription_create")) { // "subscription_cycle" is for renewals!
                Address address = charge.getBillingDetails().getAddress();
  
                dto.setEmail(invoice.getCustomerEmail());
                dto.setTitle(charge.getBillingDetails().getName());
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
                    Checkout checkout = checkoutDao.updateByHash(checkoutHash, CheckoutStatus.SUCCESSFUL.name(), null);
                    if (checkout != null) {
                      companyId = checkout.getCompanyId();
                      dto.setPlanName(checkout.getPlanName());
                      subsEvent = SubsEvent.SUBSCRIPTION_STARTED;
                    } else {
                      log.error("Failed to finish checkout! Hash: {}", checkoutHash);
                    }
                  } else {
                    log.error("Failed to get checkout info! Hash is null.");
                  }
                }
  
              } else {

                try (Handle handle = Database.getHandle()) {
                  CompanyDao companyDao = handle.attach(CompanyDao.class);
                  Company company = companyDao.findBySubsCustomerId(invoice.getCustomer());
                  if (company != null) {
                    companyId = company.getId();
                    subsEvent = SubsEvent.SUBSCRIPTION_RENEWED;
                  } else {
                    log.error("invoice.payment_succeeded: failed to find company by SubsCustomerId: {}", invoice.getCustomer());
                  }
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
                res = Responses.ServerProblem.CHECKOUT_PROBLEM;
              }
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
                  Checkout checkout = checkoutDao.updateByHash(checkoutHash, CheckoutStatus.FAILED.name(), charge.getFailureMessage());
                  if (checkout != null) {
                    companyId = checkout.getCompanyId();
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