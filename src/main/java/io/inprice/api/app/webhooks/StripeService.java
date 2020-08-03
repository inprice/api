package io.inprice.api.app.webhooks;

import com.stripe.model.Address;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.StripeObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.subscription.SubscriptionRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.SubsSource;
import io.inprice.common.models.SubsTrans;

public class StripeService {

  private static final Logger log = LoggerFactory.getLogger(StripeService.class);
  
  private static final SubscriptionRepository subsRepository = Beans.getSingleton(SubscriptionRepository.class);

  public ServiceResponse handle(Event event) {
    ServiceResponse res = Responses.BAD_REQUEST;

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
          res = subsRepository.addTransaction(companyId, invoice.getCustomer(), null, trans);
          break;
        }

        case "invoice.payment_succeeded": {
          Invoice invoice = (Invoice) stripeObject;

          try {
            Charge charge = Charge.retrieve(invoice.getCharge());
            if (charge != null) {
              InvoiceLineItem li = invoice.getLines().getData().get(0);

              CustomerInfoDTO dto = new CustomerInfoDTO();
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
                dto.setPlanId(Integer.parseInt(li.getMetadata().get("planId")));
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

              res = subsRepository.addTransaction(null, invoice.getCustomer(), dto, trans);
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
      res = new ServiceResponse("Failed to parse stripe event object!");
    }
 
    return res;
  }

}