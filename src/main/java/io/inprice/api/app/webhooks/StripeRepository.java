package io.inprice.api.app.subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.meta.SubsSource;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Plan;
import io.inprice.common.models.SubsTrans;

public class StripeRepository {

  private static final Logger log = LoggerFactory.getLogger(StripeRepository.class);

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

}
