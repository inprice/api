package io.inprice.api.app.coupon;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CompanyMapper;
import io.inprice.common.mappers.CouponMapper;
import io.inprice.common.models.Company;
import io.inprice.common.models.Coupon;
import io.inprice.common.models.SubsTrans;

interface CouponDao {

  @SqlQuery("select * from coupon where code=:code")
  @UseRowMapper(CouponMapper.class)
  Coupon findCouponByCode(@Bind String code);

  @SqlQuery("select * from company where id=:id")
  @UseRowMapper(CompanyMapper.class)
  Company findCompanyById(@Bind Long id);

  @SqlQuery("select * from coupon where issued_company_id=:issuedCompanyId order by issued_at desc")
  @UseRowMapper(CouponMapper.class)
  List<Coupon> getCoupons(@Bind Long issuedCompanyId);

  @SqlUpdate(
    "update company " + 
    "set plan_id=:planId, subs_status=:subsStatus, subs_renewal_at=DATE_ADD(now(), interval :interval day), product_limit=:productLimit " +
    "where id=:id"
  )
  boolean updateSubscription(@Bind Long id, @Bind String subsStatus, @Bind Integer interval, @Bind Integer productLimit, @Bind Integer planId);

  @SqlUpdate("update coupon set issued_at=now() where code=:code")
  boolean updateCouponByCode(@Bind String code);

  @SqlUpdate(
    "insert into subs_trans (company_id, event_source, event_id, event, successful, reason, description, file_url) " + 
    "values (:trans.companyId, :eventSource, :trans.eventId, :event, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl)"
  )
  boolean insertSubsTrans(@BindBean("trans") SubsTrans trans, @Bind String eventSource, @Bind String event);

}
