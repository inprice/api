package io.inprice.api.app.subscription;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CheckoutMapper;
import io.inprice.common.models.Checkout;

interface CheckoutDao {

  @SqlQuery("select * from checkout where _hash=:hash")
  @UseRowMapper(CheckoutMapper.class)
  Checkout findByHash(@Bind("hash") String hash);

  @SqlQuery("select * from checkout where company_id=:companyId and status in ('PENDING', 'SUCCESSFUL') order by created_at desc limit 1")
  @UseRowMapper(CheckoutMapper.class)
  Checkout findActiveCheckout(@Bind("companyId") Long companyId);

  @SqlUpdate("insert into checkout (_hash, session_id, company_id, plan_id) values (:hash, :sessionId, :companyId, :planId)")
  boolean insert(@Bind("hash") String hash, @Bind("sessionId") String sessionId, @Bind("companyId") Long companyId, @Bind("planId") Integer planId);

  @SqlUpdate("update checkout set status=:status, updated_at=now() where _hash=:hash")
  String update(@Bind("hash") String hash, @Bind("status") String status);

}
