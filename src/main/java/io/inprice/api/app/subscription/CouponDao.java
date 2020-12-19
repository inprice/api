package io.inprice.api.app.subscription;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CouponMapper;
import io.inprice.common.models.Coupon;

interface CouponDao {

  @SqlQuery("select * from coupon where code=:code")
  @UseRowMapper(CouponMapper.class)
  Coupon findByCode(@Bind("code") String code);

  @SqlUpdate("update coupon set issued_account_id=:issuedAccountId, issued_at=now() where code=:code")
  boolean applyFor(@Bind("code") String code, @Bind("issuedAccountId") Long issuedAccountId);

  @SqlUpdate(
    "insert into coupon (code, plan_name, days, description) " +
    "values (:code, :planName, :days, :description)"
  )
  boolean create(@Bind("code") String code, @Bind("planName") String planName,
    @Bind("days") Long days, @Bind("description") String description);

  @SqlQuery("select * from coupon where issued_account_id=:issuedAccountId order by issued_at desc")
  @UseRowMapper(CouponMapper.class)
  List<Coupon> findListByIssuedAccountId(@Bind("issuedAccountId") Long issuedAccountId);

}
