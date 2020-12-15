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
import io.inprice.common.meta.CompanyStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Company;
import io.inprice.common.models.CompanyTrans;
import io.inprice.common.models.Coupon;
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
      CouponDao couponDao = handle.attach(CouponDao.class);
      SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

      List<CompanyTrans> allTrans = subscriptionDao.findListByCompanyId(CurrentUser.getCompanyId());
      List<Coupon> coupons = couponDao.findListByIssuedCompanyId(CurrentUser.getCompanyId());

      data.put("all", allTrans);
      data.put("coupons", coupons);

      if (allTrans != null && allTrans.size() > 0) {
        List<CompanyTrans> invoiceTrans = new ArrayList<>();
        for (CompanyTrans st : allTrans) {
          if (st.getFileUrl() != null) {
            invoiceTrans.add(st);
          }
        }
        data.put("invoice", invoiceTrans);
      }
    }

    return new Response(data);
  }

  Response getCurrentCompany() {
    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      return new Response(companyDao.findById(CurrentUser.getCompanyId()));
    }
  }

  Response cancel() {
    Response[] res = { Responses.DataProblem.SUBSCRIPTION_PROBLEM };

    Company[] company = { null };

    //if a subscriber, stripeService will create its own transaction. 
    //so, we must not extend/cascade any db connection, thus, a new one is handled here in a separated block
    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      company[0] = companyDao.findById(CurrentUser.getCompanyId());
    }

    if (CurrentUser.getUserId().equals(company[0].getAdminId())) {
      if (company[0].getStatus().isOKForCancel()) {

        if (CompanyStatus.SUBSCRIBED.equals(company[0].getStatus())) { //if a subscriber
          res[0] = stripeService.cancel(company[0]);
        } else { //free or couponed user

          try (Handle handle = Database.getHandle()) {
            handle.inTransaction(transactional -> {
              SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);
      
              boolean isOK = subscriptionDao.terminate(company[0].getId(), CompanyStatus.CANCELLED.name());
              if (isOK) {

                CompanyTrans trans = new CompanyTrans();
                trans.setCompanyId(company[0].getId());
                trans.setSuccessful(Boolean.TRUE);
                trans.setDescription(("Manual cancelation."));
                trans.setEvent(CompanyStatus.COUPONED.equals(company[0].getStatus()) ? SubsEvent.COUPON_USE_CANCELLED : SubsEvent.FREE_USE_CANCELLED);

                isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
      
                if (isOK) {
                  CompanyDao companyDao = transactional.attach(CompanyDao.class);
                  isOK = 
                    companyDao.insertStatusHistory(
                      company[0].getId(),
                      CompanyStatus.CANCELLED.name(),
                      company[0].getPlanName(),
                      null, null
                    );
                  if (isOK) {
                    Map<String, Object> mailMap = new HashMap<>(2);
                    mailMap.put("user", CurrentUser.getEmail());
                    mailMap.put("company", StringUtils.isNotBlank(company[0].getTitle()) ? company[0].getTitle() : company[0].getName());
                    String message = templateRenderer.render(EmailTemplate.FREE_COMPANY_CANCELLED, mailMap);
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
      res[0] = Commons.refreshSession(company[0].getId());
    }

    return res[0];
  }

  Response startFreeUse() {
    Response[] res = { Responses.DataProblem.SUBSCRIPTION_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {
        CompanyDao companyDao = transactional.attach(CompanyDao.class);

        Company company = companyDao.findById(CurrentUser.getCompanyId());
        if (company != null) {

          if (!company.getStatus().equals(CompanyStatus.FREE)) {
            if (company.getStatus().isOKForFreeUse()) {
              SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

              Plan basicPlan = Plans.findById(0); // Basic Plan

              boolean isOK = 
                subscriptionDao.startFreeUseOrApplyCoupon(
                  CurrentUser.getCompanyId(),
                  CompanyStatus.FREE.name(),
                  basicPlan.getName(),
                  basicPlan.getProductLimit(),
                  Props.APP_DAYS_FOR_FREE_USE()
                );

              if (isOK) {
                CompanyTrans trans = new CompanyTrans();
                trans.setCompanyId(CurrentUser.getCompanyId());
                trans.setEvent(SubsEvent.FREE_USE_STARTED);
                trans.setSuccessful(Boolean.TRUE);
                trans.setDescription(("Free subscription has been started."));
                
                isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
                if (isOK) {
                  isOK = 
                    companyDao.insertStatusHistory(
                      company.getId(),
                      CompanyStatus.FREE.name(),
                      basicPlan.getName(),
                      null, null
                    );
                }
              }

              if (isOK) {
                res[0] = Commons.refreshSession(companyDao, company.getId());
              }

            } else {
              res[0] = Responses.Illegal.NO_FREE_USE_RIGHT;
            }
          } else {
            res[0] = Responses.Already.IN_FREE_USE;
          }
        } else {
          res[0] = Responses.NotFound.COMPANY;
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
