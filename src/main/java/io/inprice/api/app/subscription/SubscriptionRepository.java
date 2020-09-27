package io.inprice.api.app.subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.plan.PlanRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.SubsSource;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Plan;
import io.inprice.common.models.SubsTrans;

public class SubscriptionRepository {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);
  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

  public Response getInfo() {
    Company model = db.findSingle("select * from company where id=" + CurrentUser.getCompanyId(), this::companyMap);
    if (model != null)
      return new Response(model);
    else
      return Responses.NotFound.COMPANY;
  }

  public Customer updateInvoiceInfo(CustomerInfoDTO dto) {
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
      log.info("Customer info is updated, Subs Customer Id: {}, Title: {}, Email: {}", customer.getId(), dto.getTitle(), dto.getEmail());
      return customer;
    } catch (StripeException e) {
      log.error("Failed to update a new customer in Stripe", e);
    }

    return null;
  }

  public Response saveCustomerInfo(CustomerInfoDTO dto) {
    final String query = "update company set title=?, address_1=?, address_2=?, postcode=?, city=?, state=?, country=? where id=?";

    try (Connection con = db.getConnection();
        PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, dto.getTitle());
      pst.setString(++i, dto.getAddress1());
      pst.setString(++i, dto.getAddress2());
      pst.setString(++i, dto.getPostcode());
      pst.setString(++i, dto.getCity());
      pst.setString(++i, dto.getState());
      pst.setString(++i, dto.getCountry());
      pst.setLong(++i, CurrentUser.getCompanyId());

      if (pst.executeUpdate() > 0)
        return Responses.OK;
      else
        return Responses.NotFound.COMPANY;

    } catch (SQLException e) {
      log.error("Failed to set invoice info", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public Response getTransactions() {
    Map<String, List<SubsTrans>> data = new HashMap<>(2);

    List<SubsTrans> allTrans = 
      db.findMultiple(
        "select * from subs_trans where company_id=" + CurrentUser.getCompanyId() + " order by created_at desc",
      this::transMap
    );
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

    return new Response(data);
  }

  public Response addTransaction(Long companyId, String subsCustomerId, CustomerInfoDTO dto, SubsTrans trans) {
    Response res = new Response(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      // idompotency control
      SubsTrans oldTrans = db.findSingle("select * from subs_trans where event_id='" + trans.getEventId()  + "'", this::transMap);
      if (oldTrans != null) {
        log.warn("Idompotent event! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
        return Responses.DataProblem.ALREADY_EXISTS;
      }

      if (companyId == null && trans != null && trans.getCompanyId() != null) companyId = trans.getCompanyId();

      String compQuery = "select * from company";
      if (companyId != null) {
        compQuery += " where id="+companyId;
      } else if (StringUtils.isNotBlank(subsCustomerId)) {
        compQuery += " where subs_customer_id='"+subsCustomerId+"'";
      } else {
        log.warn("Company not found (1)! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
        return Responses.NotFound.COMPANY;
      }

      Company company = db.findSingle(con, compQuery, this::companyMap);
      if (company == null) {
        log.warn("Company not found (2)! Event: {}, Id: {}", trans.getEvent(), trans.getEventId());
        return Responses.NotFound.COMPANY;
      }

      if (companyId == null) companyId = company.getId();
      if (trans.getCompanyId() == null) trans.setCompanyId(companyId);

      int affected = 0;
      switch (trans.getEvent()) {

        case SUBSCRIPTION_STARTED: {
          if (company.getSubsStatus().equals(SubsStatus.ACTIVE)) return Responses.Already.ACTIVE_SUBSCRIPTION;
    
          final String query = 
            "update company " +
            "set title=?, address_1=?, address_2=?, postcode=?, city=?, state=?, country=?, " +
            "plan_id=?, subs_id=?, subs_customer_id=?, subs_status=?, subs_renewal_at=? " +
            "where id=?";
      
          try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setString(++i, dto.getTitle());
            pst.setString(++i, dto.getAddress1());
            pst.setString(++i, dto.getAddress2());
            pst.setString(++i, dto.getPostcode());
            pst.setString(++i, dto.getCity());
            pst.setString(++i, dto.getState());
            pst.setString(++i, dto.getCountry());
            pst.setInt(++i, dto.getPlanId());
            pst.setString(++i, dto.getSubsId());
            pst.setString(++i, dto.getCustId());
            pst.setString(++i, SubsStatus.ACTIVE.name());
            pst.setTimestamp(++i, dto.getRenewalDate());
            pst.setLong(++i, companyId);
            affected = pst.executeUpdate();
            if (affected > 0) {
              if (trans.getEvent().equals(SubsEvent.SUBSCRIPTION_STARTED)) updateInvoiceInfo(dto);
              log.info("A new subscription is started: Title: {}, Company Id: {}", dto.getTitle(), companyId);
            }
          }
          break;
        }

        case SUBSCRIPTION_RENEWAL: {
          final String query = 
            "update company " +
            "set subs_status=?, subs_renewal_at=? " +
            "where id=?";
      
          try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setString(++i, SubsStatus.ACTIVE.name());
            pst.setTimestamp(++i, dto.getRenewalDate());
            pst.setLong(++i, companyId);
            affected = pst.executeUpdate();
            if (affected > 0) {
              log.info("Subscription is renewed: Company Id: {}", companyId);
            }
          }
          break;
        }

        case COUPON_USED: {
          if (company.getSubsStatus().equals(SubsStatus.ACTIVE)) return Responses.Already.ACTIVE_SUBSCRIPTION;

          final String query = 
            "update company " +
            "set plan_id=?, subs_status=?, subs_renewal_at=? " +
            "where id=?";
      
          try (PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setInt(++i, dto.getPlanId());
            pst.setString(++i, SubsStatus.COUPONED.name());
            pst.setTimestamp(++i, dto.getRenewalDate());
            pst.setLong(++i, companyId);
            affected = pst.executeUpdate();
            if (affected > 0) {
              log.info("Coupon is used: Company Id: {}, Coupon: {}", companyId, trans.getEventId());
            }
          }
          break;
        }

        case SUBSCRIPTION_CANCELLED: {
          if (company.getSubsStatus().equals(SubsStatus.ACTIVE) || company.getSubsStatus().equals(SubsStatus.COUPONED)) {
            if (company.getSubsStatus().equals(SubsStatus.ACTIVE)) {
              trans.setSource(SubsSource.SUBSCRIPTION);
            } else {
              trans.setSource(SubsSource.COUPON);
            }

            final String query = 
              "update company " +
              "set subs_status=?, subs_renewal_at=now() " +
              "where id=?";
        
            try (PreparedStatement pst = con.prepareStatement(query)) {
              int i = 0;
              pst.setString(++i, SubsStatus.CANCELLED.name());
              pst.setLong(++i, companyId);
              affected = pst.executeUpdate();
              if (affected > 0) {
                log.info("Subscription is cancelled: Company Id: {}", companyId);
              }
            }
          } else {
            return Responses.Already.PASSIVE_SUBSCRIPTION;
          }
          break;
        }

        case PAYMENT_FAILED: {
          affected = 1; // must be set 1 so that the process placed just after this switch block can continue
          log.info("Payment failed! Company Id: {}", companyId);
          break;
        }

      }

      if (affected > 0) {
        if (insertTrans(con, trans) > 0) {
          res = Responses.OK;
          db.commit(con);
        } else {
          db.rollback(con);
        }
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to handle subscription trans. Company Id: " + companyId, e);
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

  public int insertTrans(Connection con, SubsTrans trans) {
    final String query = "insert into subs_trans (company_id, event_source, event_id, event, successful, reason, description, file_url) values (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setLong(++i, trans.getCompanyId());
      pst.setString(++i, trans.getSource().name());
      pst.setString(++i, trans.getEventId());
      pst.setString(++i, trans.getEvent().getEventDesc());
      pst.setBoolean(++i, trans.getSuccessful());
      pst.setString(++i, trans.getReason());
      pst.setString(++i, trans.getDescription());
      pst.setString(++i, trans.getFileUrl());
      return pst.executeUpdate();
    } catch (SQLException e) {
      log.error("Failed to insert a new trans.", e);
    }
    return 0;
  }

  public Response createSession(Integer planId) {
    Plan plan = planRepository.findById(planId);

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
        Map<String, String> data = new HashMap<>(1);
        data.put("sessionId", session.getId());
        return new Response(data);
      } catch (StripeException e) {
        log.error("Failed to create checkout session", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }

    return Responses.NotFound.PLAN;
  }

  private Company companyMap(ResultSet rs) {
    try {
      Company model = new Company();
      model.setTitle(rs.getString("title"));
      model.setAddress1(rs.getString("address_1"));
      model.setAddress2(rs.getString("address_2"));
      model.setPostcode(rs.getString("postcode"));
      model.setCity(rs.getString("city"));
      model.setState(rs.getString("state"));
      model.setCountry(rs.getString("country"));
      model.setPlanId(RepositoryHelper.nullIntegerHandler(rs, "plan_id"));
      model.setProductLimit(rs.getInt("product_limit"));
      model.setSubsStatus(SubsStatus.valueOf(rs.getString("subs_status")));
      model.setSubsCustomerId(rs.getString("subs_customer_id"));
      model.setSubsRenewalAt(rs.getTimestamp("subs_renewal_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set company properties", e);
    }
    return null;
  }

  private SubsTrans transMap(ResultSet rs) {
    try {
      SubsTrans model = new SubsTrans();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setEventId(rs.getString("event_id"));
      model.setEvent(SubsEvent.findByDescription(rs.getString("event")));
      model.setSuccessful(rs.getBoolean("successful"));
      model.setReason(rs.getString("reason"));
      model.setDescription(rs.getString("description"));
      model.setFileUrl(rs.getString("file_url"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set subs trans's properties", e);
    }
    return null;
  }

}
