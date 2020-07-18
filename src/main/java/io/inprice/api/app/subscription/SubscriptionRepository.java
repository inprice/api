package io.inprice.api.app.subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.InvoiceInfoDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.EventType;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.SubsEvent;

public class SubscriptionRepository {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public ServiceResponse getInvoiceInfo() {
    Company model = db.findSingle("select * from company where id=" + CurrentUser.getCompanyId(), this::map);
    if (model != null)
      return new ServiceResponse(model);
    else
      return Responses.NotFound.COMPANY;
  }

  public ServiceResponse updateInvoiceInfo(InvoiceInfoDTO dto) {
    final String query = "update company set title=?, address_1=?, address_2=?, town_or_city=?, postcode=?, country=? where id=?";

    try (Connection con = db.getConnection();
        PreparedStatement pst = con.prepareStatement(query)) {
      int i = 0;
      pst.setString(++i, dto.getTitle());
      pst.setString(++i, dto.getAddress1());
      pst.setString(++i, dto.getAddress2());
      pst.setString(++i, dto.getTownOrCity());
      pst.setString(++i, dto.getPostcode());
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

  public ServiceResponse cancel() {
    ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      Company company = 
        db.findSingle(con, 
          String.format("select * from company where id=%d", CurrentUser.getCompanyId()),
          this::mapForCompany
      );

      if (company.getSubsStatus().equals(SubsStatus.ACTIVE) || company.getSubsStatus().equals(SubsStatus.COUPONED)) {
        try (PreparedStatement pst = con.prepareStatement("update company set subs_status=?, subs_renewal_at=now() where id=?")) {
          int i = 0;
          pst.setString(++i, SubsStatus.CANCELLED.name());
          pst.setLong(++i, CurrentUser.getCompanyId());

          if (pst.executeUpdate() > 0) {
            if (company.getSubsStatus().equals(SubsStatus.ACTIVE)) {
              final String insertQuery = "insert into subs_event (company_id, event_type, event, data) values (?, ?, ?, ?)";
              try (PreparedStatement pst1 = con.prepareStatement(insertQuery)) {
                int j = 0;
                pst1.setLong(++j, CurrentUser.getCompanyId());
                if (company.getSubsStatus().equals(SubsStatus.ACTIVE)) {
                  pst1.setString(++j, EventType.SUBSCRIPTION.name());
                  pst1.setString(++j, "subscription.cancel");
                }
                pst1.setString(++j, "{ \"description\": \"Manually cancelled\" }");
    
                if (pst1.executeUpdate() > 0) {
                  res = cancel(con);
                }
              }
              log.info("A subscription is canceled: Company: {}, Id: {}", company.getName(), company.getId());

            } else {
              res = cancel(con);
              log.info("A coupon is canceled: Company: {}, Id: {}", company.getName(), company.getId());
            }
          }
        }
      } else {
        res = Responses.Already.PASSIVE_SUBSCRIPTION;
      }

      if (res.isOK()) {
        db.commit(con);
      } else {
        db.rollback(con);
      }

    } catch (SQLException e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to cancel subscription. Company Id: " + CurrentUser.getCompanyId(), e);
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

  public ServiceResponse getTransactions() {
    return 
      new ServiceResponse(
        db.findMultiple("select * from subs_event where company_id=" + CurrentUser.getCompanyId() + " order by created_at desc", this::transMap)
      );
  }

  private ServiceResponse cancel(Connection con) {
    List<String> hashes = db.findMultiple(con,
        String.format("select _hash from user_session where company_id=%d", CurrentUser.getCompanyId()), this::mapForHashField);

    if (hashes != null && hashes.size() > 0) {
      Map<String, ForRedis> map = new HashMap<>(hashes.size());
      for (String hash : hashes) {
        ForRedis ses = RedisClient.getSession(hash);
        if (ses == null) {
          RedisClient.removeSesion(hash);
          continue;
        }
        ses.setPlanId(ses.getPlanId());
        ses.setSubsStatus(SubsStatus.CANCELLED);
        ses.setSubsRenewalAt(new Date());
        map.put(hash, ses);
      }
      RedisClient.updateSessions(map);

      return Responses.OK;
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

  private Company mapForCompany(ResultSet rs) {
    try {
      Company model = new Company();
      model.setName(rs.getString("name"));
      model.setPlanId(RepositoryHelper.nullLongHandler(rs, "plan_id"));
      model.setProductLimit(rs.getInt("product_limit"));
      model.setSubsStatus(SubsStatus.valueOf(rs.getString("subs_status")));
      model.setSubsRenewalAt(rs.getTimestamp("subs_renewal_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set company's properties for subscription", e);
    }
    return null;
  }

  private SubsEvent transMap(ResultSet rs) {
    try {
      SubsEvent model = new SubsEvent();
      model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
      model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
      model.setEventId(rs.getString("event_id"));
      model.setEventType(EventType.valueOf(rs.getString("event_type")));
      model.setEvent(rs.getString("event"));
      model.setData(rs.getString("data"));
      model.setCreatedAt(rs.getTimestamp("created_at"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set transaction's properties", e);
    }
    return null;
  }

  private Company map(ResultSet rs) {
    try {
      Company model = new Company();
      model.setTitle(rs.getString("title"));
      model.setAddress1(rs.getString("address_1"));
      model.setAddress2(rs.getString("address_2"));
      model.setTownOrCity(rs.getString("town_or_city"));
      model.setPostcode(rs.getString("postcode"));
      model.setCountry(rs.getString("country"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set invoice info's properties", e);
    }
    return null;
  }

  private String mapForHashField(ResultSet rs) {
    try {
      return rs.getString("_hash");
    } catch (SQLException e) {
      log.error("Failed to get _hash field from user_session table", e);
    }
    return null;
  }

}
