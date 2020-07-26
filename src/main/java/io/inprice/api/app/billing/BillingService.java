package io.inprice.api.app.billing;


import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;

public class BillingService {

  private static final Logger log = LoggerFactory.getLogger(BillingService.class);

  private static final BillingRepository repository = Beans.getSingleton(BillingRepository.class);

  public ServiceResponse getInfo() {
    return repository.getInfo();
  }

  public ServiceResponse getTransactions() {
    return repository.getTransactions();
  }

  public ServiceResponse saveInfo(CustomerInfoDTO dto) {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "DB error!");

    
    String problem = validateInvoiceInfo(dto);
    if (problem == null) {

      Customer customer = null;

      // create
      if (StringUtils.isBlank(dto.getCustomerId())) {

        CustomerCreateParams.Address address =
          CustomerCreateParams.Address.builder()
            .setLine1(dto.getAddress1())
            .setLine2(dto.getAddress2())
            .setPostalCode(dto.getPostcode())
            .setCity(dto.getCity())
            .setState(dto.getState())
            .setCountry(dto.getCountry())
          .build();

        CustomerCreateParams customerParams =
          CustomerCreateParams.builder()
            .setName(dto.getTitle())
            .setEmail(CurrentUser.getEmail())
            .setAddress(address)
          .build();

        try {
          customer = Customer.create(customerParams);
          res = Responses.OK;
          log.info("A new customer is created for " + CurrentUser.getCompanyName() + ", Id: " + customer.getId());
        } catch (StripeException e) {
          log.error("Failed to create a new customer in Stripe", e);
          res = new ServiceResponse("Sorry, we are unable to init your invoice info at the moment. We are working on it.");
        }
      } // update
      else {

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

        try {
          customer = Customer.retrieve(dto.getCustomerId()).update(customerParams);
          res = Responses.OK;
          log.info( CurrentUser.getCompanyName() + " customer info is updated, Id: " + customer.getId());
        } catch (StripeException e) {
          log.error("Failed to update a new customer in Stripe", e);
          res = new ServiceResponse("Sorry, we are unable to update your invoice info at the moment. We are working on it.");
        }
      }

      if (res.isOK() && customer != null) {
        dto.setCustomerId(customer.getId());
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
