package io.inprice.api.app.subscription;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CheckoutMapper;
import io.inprice.common.models.Checkout;

public interface CheckoutDao {

  @SqlQuery("select * from checkout where _hash=:hash")
  @UseRowMapper(CheckoutMapper.class)
  Checkout findByHash(@Bind("hash") String hash);

  @SqlQuery("select * from checkout where workspace_id=:workspaceId order by created_at desc limit 1")
  @UseRowMapper(CheckoutMapper.class)
  Checkout findCheckedAtout(@Bind("workspaceId") Long workspaceId);

  @SqlUpdate("insert into checkout (_hash, session_id, workspace_id, plan_id) values (:hash, :sessionId, :workspaceId, :planId)")
  boolean insert(@Bind("hash") String hash, @Bind("sessionId") String sessionId, @Bind("workspaceId") Long workspaceId, @Bind("planId") Integer planId);

  @SqlUpdate("update checkout set status=:status, description=:description, updated_at=now() where _hash=:hash and status = 'PENDING'")
  boolean update(@Bind("hash") String hash, @Bind("status") String status, @Bind("description") String description);

}
