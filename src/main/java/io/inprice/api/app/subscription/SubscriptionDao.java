package io.inprice.api.app.subscription;

import java.sql.Timestamp;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.CustomerDTO;
import io.inprice.common.mappers.CompanyHistoryMapper;
import io.inprice.common.mappers.CompanyTransMapper;
import io.inprice.common.models.CompanyHistory;
import io.inprice.common.models.CompanyTrans;

public interface SubscriptionDao {

  @SqlQuery("select * from company_trans where event_id=:eventId")
  @UseRowMapper(CompanyTransMapper.class)
  CompanyTrans findByEventId(@Bind("eventId") String eventId);

  @SqlQuery("select * from company_trans where company_id=:companyId order by id desc")
  @UseRowMapper(CompanyTransMapper.class)
  List<CompanyTrans> findListByCompanyId(@Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into company_trans (company_id, event_id, successful, reason, description, file_url, event) " + 
    "values (:trans.companyId, :trans.eventId, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl, :event)"
  )
  boolean insertTrans(@BindBean("trans") CompanyTrans trans, @Bind("event") String event);

  @SqlQuery("select * from company_history where company_id=:companyId and status=:status order by id desc limit 1")
  @UseRowMapper(CompanyHistoryMapper.class)
  CompanyHistory findPreviousHistoryRowByStatusAndCompanyId(@Bind("companyId") Long companyId, @Bind("status") String status);

  @SqlUpdate(
    "update company " +
    "set title=:dto.title, address_1=:dto.address1, address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country, plan_name=:dto.planName, " +
    "product_limit=:productLimit, subs_id=:dto.subsId, cust_id=:dto.custId, status=:status, renewal_at=:dto.renewalDate, subs_started_at=now(), last_status_update=now() " +
    "where id=:id"
  )
  boolean startSubscription(@BindBean("dto") CustomerDTO dto, @Bind("status") String status, @Bind("productLimit") Integer productLimit, @Bind("id") Long id);

  @SqlUpdate("update company set status=:status, renewal_at=:renewalAt where id=:id")
  boolean renewSubscription(@Bind("id") Long id, @Bind("status") String status, @Bind("renewalAt") Timestamp renewalAt);

  @SqlUpdate(
    "update company " + 
    "set plan_name=:planName, status=:status, renewal_at=DATE_ADD(now(), interval <interval> day), product_limit=:productLimit, last_status_update=now() " +
    "where id=:companyId"
  )
  boolean startFreeUseOrApplyCoupon(@Bind("companyId") Long companyId, @Bind("status") String status, 
    @Bind("planName") String planName, @Bind("productLimit") Integer productLimit, @Define("interval") Integer interval);

  @SqlUpdate(
    "update company " +
    "set subs_id=null, plan_name=null, renewal_at=null, status=:status, last_status_update=now() "+
    "where id=:id"
  )
  boolean terminate(@Bind("id") Long id, @Bind("status") String status);

  @SqlUpdate(
    "update company " + 
    "set plan_name=:planName, product_limit=:productLimit, subs_started_at=now(), last_status_update=now() " +
    "where id=:companyId"
  )
  boolean changePlan(@Bind("companyId") Long companyId, @Bind("planName") String planName, @Bind("productLimit") Integer productLimit);

}
