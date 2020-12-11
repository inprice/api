package io.inprice.api.app.subscription;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import io.inprice.common.mappers.CheckoutMapper;
import io.inprice.common.models.Checkout;

public interface CheckoutDao {

  @SqlQuery(
    "select * from checkout " +
    "where company_id=:companyId " +
    "order by created_at desc " +
    "limit 1"
  )
  @UseRowMapper(CheckoutMapper.class)
  Checkout findLastCheckout(@Bind("companyId") Long companyId);

  @SqlUpdate("insert into checkout (_hash, session_id, company_id, plan_name) values (:hash, :sessionId, :companyId, :planName)")
  boolean insert(@Bind("hash") String hash, @Bind("sessionId") String sessionId, @Bind("companyId") Long companyId, @Bind("planName") String planName);

  @SqlUpdate("update checkout set status=:status, description=:description, updated_at=now() where _hash=:hash and status = 'PENDING'")
  @UseRowMapper(CheckoutMapper.class)
  Checkout updateByHash(@Bind("hash") String hash, @Bind("status") String status, @Bind("description") String description);

  @Transaction
  @SqlUpdate(
    "update checkout " +
    "set status='EXPIRED', description='Expired by system.', updated_at=now() " +
    "where status = 'PENDING' " + 
    "  and created_at <= now() - interval 2 hour"
  )
  int expirePendings();

}
