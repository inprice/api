package io.inprice.api.app.subscription;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.StripeCustomerDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;

public class SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  private static final SubscriptionRepository repository = Beans.getSingleton(SubscriptionRepository.class);

  public ServiceResponse getTransactions() {
    return repository.getTransactions();
  }

  public ServiceResponse cancel() {
    return repository.cancel();
  }

  public ServiceResponse saveCustomer(StripeCustomerDTO dto) {
    String problem = validateCustomer(dto);
    if (problem == null) {

      // create
      if (StringUtils.isBlank(dto.getId())) {

        CustomerCreateParams.Address address =
          CustomerCreateParams.Address.builder()
            .setLine1(dto.getLine1())
            .setLine2(dto.getLine2())
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
          Customer customer = Customer.create(customerParams);
          System.out.println("Created stripe's customer id: " + customer.getId());
        } catch (StripeException e) {
          log.error("Failed to create a new customer in Stripe", e);
        }
      } // update
      else {

        CustomerUpdateParams.Address address =
          CustomerUpdateParams.Address.builder()
            .setLine1(dto.getLine1())
            .setLine2(dto.getLine2())
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
          Customer customer = Customer.retrieve(dto.getId()).update(customerParams);
          System.out.println("Updated stripe's customer id: " + customer.getId());
        } catch (StripeException e) {
          log.error("Failed to update a customer in Stripe", e);
        }
      }

    }

    return Responses.OK;
  }

  private String validateCustomer(StripeCustomerDTO dto) {
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
      if (StringUtils.isBlank(dto.getLine1())) {
        problem = "Address line 1 cannot be null!";
      } else if (dto.getLine1().length() > 255) {
        problem = "Address line 1 must be less than 255 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isNotBlank(dto.getLine1()) && dto.getLine1().length() > 255) {
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
