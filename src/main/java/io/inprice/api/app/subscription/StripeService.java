package io.inprice.api.app.subscription;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import com.stripe.exception.StripeException;
import com.stripe.model.Address;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.StripeObject;
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
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.SubsSource;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Plan;
import io.inprice.common.models.SubsTrans;

class StripeService {

  private static final Logger log = LoggerFactory.getLogger(StripeService.class);

  Response createCheckoutSession(Integer planId) {
    Plan plan = Plans.findById(planId);

    if (plan != null) {
      SessionCreateParams params = SessionCreateParams.builder()
        .setCustomerEmail(CurrentUser.getEmail())
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
        .setSuccessUrl(Props.APP_WEB_URL() + "/payment-ok")
        .setCancelUrl(Props.APP_WEB_URL() + "/payment-cancel")
        .setClientReferenceId(""+planId)
        .setSubscriptionData(
          SessionCreateParams.SubscriptionData
          .builder()
              .putMetadata("planId", ""+planId)
              .putMetadata("companyId", ""+CurrentUser.getCompanyId()
            ).build())
        .addLineItem(
          SessionCreateParams.LineItem
            .builder()  
              .setQuantity(1L)
              .setPrice(plan.getStripePriceId()
            ).build()
          )
        .build();

       try {
        Session session = Session.create(params);
        return new Response(Collections.singletonMap("sessionId", session.getId()));
      } catch (StripeException e) {
        log.error("Failed to create checkout session", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }

    return Responses.NotFound.PLAN;
  }

  Response cancel(SubsTrans trans) {
    return addTransaction(CurrentUser.getCompanyId(), null, null, trans);
  }
  
  Response handleHookEvent(Event event) {
    Response res = Responses.BAD_REQUEST;

    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    StripeObject stripeObject = null;
    if (dataObjectDeserializer.getObject().isPresent()) {
      stripeObject = dataObjectDeserializer.getObject().get();
    }

    if (stripeObject != null) {
      switch (event.getType()) {
        case "invoice.payment_failed": {
          SubsTrans trans = new SubsTrans();
          Long companyId = null;
          Invoice invoice = (Invoice) stripeObject;
          if (invoice.getLines() != null && invoice.getLines().getData() != null && invoice.getLines().getData().size() > 0) {
            InvoiceLineItem li = invoice.getLines().getData().get(0);
            trans.setDescription(li.getDescription());
            if (li.getMetadata().size() > 0) {
              companyId = Long.parseLong(li.getMetadata().get("companyId"));
            }
          }
          trans.setEventId(event.getId());
          trans.setEvent(SubsEvent.PAYMENT_FAILED);
          trans.setSource(SubsSource.SUBSCRIPTION);
          trans.setSuccessful(Boolean.FALSE);
          trans.setReason(invoice.getBillingReason());
          trans.setFileUrl(invoice.getHostedInvoiceUrl());
          res = addTransaction(companyId, invoice.getCustomer(), null, trans);
          break;
        }

        case "invoice.payment_succeeded": {
          Invoice invoice = (Invoice) stripeObject;

          try {
            Charge charge = Charge.retrieve(invoice.getCharge());
            if (charge != null) {
              InvoiceLineItem li = invoice.getLines().getData().get(0);

              CustomerDTO dto = new CustomerDTO();
              dto.setRenewalDate(new java.sql.Timestamp(li.getPeriod().getEnd() * 1000));

              SubsEvent subsEvent = null;
              if (invoice.getBillingReason().equals("subscription_create")) {
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
                dto.setPlanName(Plans.findById(Integer.parseInt(li.getMetadata().get("planId"))).getName());
                subsEvent = SubsEvent.SUBSCRIPTION_STARTED;
              } else {
                subsEvent = SubsEvent.SUBSCRIPTION_RENEWAL;
              }

              Long companyId = null;
              if (li.getMetadata() != null && li.getMetadata().size() > 0) {
                companyId = Long.parseLong(li.getMetadata().get("companyId"));
              }

              SubsTrans trans = new SubsTrans();
              trans.setCompanyId(companyId);
              trans.setEventId(event.getId());
              trans.setEvent(subsEvent);
              trans.setSource(SubsSource.SUBSCRIPTION);
              trans.setSuccessful(Boolean.TRUE);
              trans.setReason(invoice.getBillingReason());
              trans.setDescription(li.getDescription());
              trans.setFileUrl(invoice.getHostedInvoiceUrl());

              res = addTransaction(null, invoice.getCustomer(), dto, trans);
            }

          } catch (Exception e) {
            log.error("An error occurred.", e);
            res = Responses.ServerProblem.EXCEPTION;
          }

          break;
        }
        default: {
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

  Response addTransaction(Long company_id, String subsCustomerId, CustomerDTO dto, SubsTrans trans) {
    Response[] res = { Responses.DataProblem.SUBSCRIPTION_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {
        CompanyDao companyDao = transactional.attach(CompanyDao.class);
        SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

        SubsTrans oldTrans = subscriptionDao.findByEventId(trans.getEventId());
        if (oldTrans == null) {

          Long companyId = null;
          if (company_id == null && trans != null && trans.getCompanyId() != null) companyId = trans.getCompanyId();

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
                if (company.getSubsStatus().isOKForSubscription()) {
                  if (companyDao.update(dto, SubsStatus.SUBSCRIBED.name(), companyId)) {
                    boolean isOK = updateInvoiceInfo(dto);
                    if (isOK) {
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
      
              case SUBSCRIPTION_RENEWAL: {
                if (companyDao.updateSubscription(SubsStatus.SUBSCRIBED.name(), dto.getRenewalDate(), companyId)) {
                  res[0] = Responses.OK;
                  log.info("Subscription is renewed: Company Id: {}", companyId);
                } else {
                  log.warn("Failed to renew a subscription: Company Id: {}", companyId);
                }
                break;
              }
      
              case COUPON_USED: {
                if (company.getSubsStatus().isOKForCoupon()) {
                  if (companyDao.update(dto, SubsStatus.COUPONED.name(), companyId)) {
                    res[0] = Responses.OK;
                    log.info("Coupon is used: Company Id: {}, Coupon: {}", companyId, trans.getEventId());
                  } else {
                    log.warn("Failed to assign a coupon: Company Id: {}, Coupon: {}", companyId, trans.getEventId());
                  }
                } else {
                  res[0] = Responses.Already.ACTIVE_SUBSCRIPTION;
                }
                break;
              }
      
              case SUBSCRIPTION_CANCELLED: {
                if (company.getSubsStatus().isOKForCancel()) {
                  if (company.getSubsStatus().equals(SubsStatus.COUPONED)) {
                    trans.setSource(SubsSource.COUPON);
                  } else {
                    trans.setSource(SubsSource.SUBSCRIPTION);
                  }
      
                  if (companyDao.updateSubscription(SubsStatus.CANCELLED.name(), new Timestamp(new Date().getTime()), companyId)) {
                    res[0] = Responses.OK;
                    log.info("Subscription is cancelled: Company Id: {}", companyId);
                  } else {
                    log.warn("Failed to cancel a subscription: Company Id: {}", companyId);
                  }

                } else {
                  res[0] = Responses.Already.PASSIVE_SUBSCRIPTION;
                }
                break;
              }
      
              case PAYMENT_FAILED: {
                res[0] = Responses.OK; // must be set true so that the transactional can reflect on database
                log.warn("Payment failed! Company Id: {}", companyId);
                break;
              }
      
            }
      
            if (res[0].isOK()) {
              boolean isAllRight = subscriptionDao.insertTrans(trans, trans.getSource().name(), trans.getEvent().name());
              if (! isAllRight) {
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