package io.inprice.api.app.subscription;

import com.stripe.model.Customer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.SubsTrans;

public class SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  private static final SubscriptionRepository repository = Beans.getSingleton(SubscriptionRepository.class);

  public ServiceResponse getInfo() {
    return repository.getInfo();
  }

  public ServiceResponse getTransactions() {
    return repository.getTransactions();
  }

  public ServiceResponse cancel() {
    SubsTrans trans = new SubsTrans();
    trans.setCompanyId(CurrentUser.getCompanyId());
    trans.setEvent(SubsEvent.SUBSCRIPTION_CANCELLED);
    trans.setSuccessful(Boolean.TRUE);
    trans.setReason(("subscription_cancel"));
    trans.setDescription(("Manual cancellation."));
    return repository.addTransaction(CurrentUser.getCompanyId(), null, null, trans);
  }
  
  public ServiceResponse createSession(Integer planId) {
    return repository.createSession(planId);
  }

  public ServiceResponse saveInfo(CustomerInfoDTO dto) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "DB error!");

    String problem = validateInvoiceInfo(dto);
    if (problem == null) {

      dto.setEmail(CurrentUser.getEmail());
      Customer customer = repository.updateInvoiceInfo(dto);

      if (customer != null) {
        res = Responses.OK;
        log.info(CurrentUser.getCompanyName() + " customer info is updated, Id: " + customer.getId());
      } else {
        res = new ServiceResponse("Sorry, we are unable to update your invoice info at the moment. We are working on it.");
        log.error("Failed to update a new customer in Stripe.");
      }

      if (res.isOK() && customer != null) {
        dto.setCustId(customer.getId());
        res = repository.saveCustomerInfo(dto);
      }

    } else {
      res = new ServiceResponse(problem);
    }

    return res;
  }

  private String validateInvoiceInfo(CustomerInfoDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid customer data!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCustId())) {
        problem = "Customer id cannot be null!";
      }
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
