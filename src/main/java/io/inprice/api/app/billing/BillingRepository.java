package io.inprice.api.app.billing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerInfoDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.EventType;
import io.inprice.common.models.Company;
import io.inprice.common.models.SubsEvent;

public class BillingRepository {

  private static final Logger log = LoggerFactory.getLogger(BillingRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public ServiceResponse getInfo() {
    Company model = db.findSingle("select * from company where id=" + CurrentUser.getCompanyId(), this::map);
    if (model != null)
      return new ServiceResponse(model);
    else
      return Responses.NotFound.COMPANY;
  }

  public ServiceResponse getTransactions() {
    return 
      new ServiceResponse(
        db.findMultiple("select * from subs_event where company_id=" + CurrentUser.getCompanyId() + " order by created_at desc", this::transMap)
      );
  }

  public ServiceResponse saveCustomerInfo(CustomerInfoDTO dto) {
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
      model.setSubsCustomerId(rs.getString("subs_customer_id"));
      model.setTitle(rs.getString("title"));
      model.setAddress1(rs.getString("address_1"));
      model.setAddress2(rs.getString("address_2"));
      model.setPostcode(rs.getString("postcode"));
      model.setCity(rs.getString("city"));
      model.setState(rs.getString("state"));
      model.setCountry(rs.getString("country"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set invoice info's properties", e);
    }
    return null;
  }

}
