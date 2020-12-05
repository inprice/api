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

  @SqlUpdate("update coupon set issued_company_id=:issuedCompanyId, issued_at=now() where code=:code")
  boolean applyFor(@Bind("code") String code, @Bind("issuedCompanyId") Long issuedCompanyId);

  @SqlQuery("select * from coupon where issued_company_id=:issuedCompanyId order by issued_at desc")
  @UseRowMapper(CouponMapper.class)
  List<Coupon> findListByIssuedCompanyId(@Bind("issuedCompanyId") Long issuedCompanyId);

}
