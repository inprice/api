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

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.SubsTrans;

class SubscriptionService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

  Response getCurrentCompany() {
    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      return new Response(companyDao.findById(CurrentUser.getCompanyId()));
    }
  }

  Response getTransactions() {
    Map<String, List<SubsTrans>> data = new HashMap<>(2);

    try (Handle handle = Database.getHandle()) {
      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

      List<SubsTrans> allTrans = subscriptionDao.findTransListByCompanyId(CurrentUser.getCompanyId());
      data.put("all", allTrans);

      if (allTrans != null && allTrans.size() > 0) {
        List<SubsTrans> invoiceTrans = new ArrayList<>();
        for (SubsTrans st : allTrans) {
          if (st.getFileUrl() != null) {
            invoiceTrans.add(st);
          }
        }
        data.put("invoice", invoiceTrans);
      }
    }

    return new Response(data);
  }

  SubsTrans getCancellationTrans() {
    SubsTrans trans = new SubsTrans();
    trans.setCompanyId(CurrentUser.getCompanyId());
    trans.setEvent(SubsEvent.SUBSCRIPTION_CANCELLED);
    trans.setSuccessful(Boolean.TRUE);
    trans.setReason(("subscription_cancel"));
    trans.setDescription(("Manual cancellation."));
    return trans;
  }

  Response saveInfo(CustomerDTO dto) {
    Response res = new Response("Sorry, we are unable to update your invoice info at the moment. We are working on it.");

    String problem = validateInvoiceInfo(dto);
    if (problem == null) {

      dto.setEmail(CurrentUser.getEmail());

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

      Customer customer = null;

      try {
        customer = Customer.retrieve(dto.getCustId()).update(customerParams);
        if (customer != null) {
          res = Responses.OK;
          log.info(CurrentUser.getCompanyName() + " customer info is updated, Id: " + customer.getId());
          log.info("Customer info is updated. Company: {}, Subs Customer Id: {}, Title: {}, Email: {}", 
            CurrentUser.getCompanyName(), customer.getId(), dto.getTitle(), dto.getEmail());
        } else {
          log.error("Failed to update a new customer in Stripe.");
        }

      } catch (StripeException e) {
        log.error("Failed to update a new customer in Stripe", e);
        log.error("Company: {}, Title: {}, Email: {}", CurrentUser.getCompanyName(), dto.getTitle(), dto.getEmail());
      }

      if (res.isOK() && customer != null) {
        dto.setCustId(customer.getId());

        try (Handle handle = Database.getHandle()) {
          CompanyDao dao = handle.attach(CompanyDao.class);
          boolean isOK = dao.update(dto, CurrentUser.getCompanyId());
          if (isOK) {
            return Responses.OK;
          } else {
            return Responses.NotFound.COMPANY;
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
