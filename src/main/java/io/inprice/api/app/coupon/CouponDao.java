package io.inprice.api.app.coupon;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CouponMapper;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.SubsTrans;

interface CouponDao {

  @SqlQuery("select * from coupon where code=:code")
  @UseRowMapper(CouponMapper.class)
  Coupon findByCode(@Bind("code") String code);

  @SqlUpdate("update coupon set issued_at=now() where code=:code")
  boolean updateByCode(@Bind("code") String code);

  @SqlQuery("select * from coupon where issued_company_id=:issuedCompanyId order by issued_at desc")
  @UseRowMapper(CouponMapper.class)
  List<Coupon> findListByIssuedCompanyId(@Bind("issuedCompanyId") Long issuedCompanyId);

  @SqlUpdate(
    "update company " + 
    "set plan_id=:planId, subs_status=:subsStatus, subs_renewal_at=DATE_ADD(now(), interval :interval day), product_limit=:productLimit " +
    "where id=:id"
  )
  boolean updateSubscription(@Bind("id") Long id, @Bind("subsStatus") String subsStatus, @Bind("interval") Integer interval, 
    @Bind("productLimit") Integer productLimit, @Bind("planId") Integer planId);

  @SqlUpdate(
    "insert into subs_trans (company_id, event_source, event_id, event, successful, reason, description, file_url) " + 
    "values (:trans.companyId, :eventSource, :trans.eventId, :event, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl)"
  )
  boolean insertSubsTrans(@BindBean("trans") SubsTrans trans, @Bind("eventSource") String eventSource, @Bind("event") String event);

}
