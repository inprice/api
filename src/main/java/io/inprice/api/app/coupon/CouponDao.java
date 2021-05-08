package io.inprice.api.app.coupon;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CouponMapper;
import io.inprice.common.models.Coupon;

public interface CouponDao {

  @SqlQuery("select * from coupon where code=:code")
  @UseRowMapper(CouponMapper.class)
  Coupon findByCode(@Bind("code") String code);

  @SqlUpdate("update coupon set issued_id=:issuedId, issued_at=now() where code=:code")
  boolean applyFor(@Bind("code") String code, @Bind("issuedId") Long issuedId);

  @SqlUpdate(
    "insert into coupon (code, plan_id, days, description, issuer_id) " +
    "values (:code, :planId, :days, :description, :issuerId)"
  )
  boolean create(@Bind("code") String code, @Bind("planId") Integer planId,
    @Bind("days") Long days, @Bind("description") String description, @Bind("issuerId") Long issuerId);

  @SqlQuery(
    "select * from coupon " + 
    "where (issued_id=:accountId) " + 
    "   or (issuer_id=:accountId and issued_id is null) " +
    "order by issued_at desc"
  )
  @UseRowMapper(CouponMapper.class)
  List<Coupon> findListByAccountId(@Bind("accountId") Long accountId);

}
