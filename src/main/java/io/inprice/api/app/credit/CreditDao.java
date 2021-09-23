package io.inprice.api.app.credit;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CreditMapper;
import io.inprice.common.models.Credit;

public interface CreditDao {

  @SqlQuery("select * from credit where code=:code")
  @UseRowMapper(CreditMapper.class)
  Credit findByCode(@Bind("code") String code);

  @SqlUpdate("update credit set issued_id=:issuedId, issued_at=now() where code=:code")
  boolean applyFor(@Bind("code") String code, @Bind("issuedId") Long issuedId);

  @SqlUpdate(
    "insert into credit (code, plan_id, days, description, issuer_id) " +
    "values (:code, :planId, :days, :description, :issuerId)"
  )
  boolean create(@Bind("code") String code, @Bind("planId") Integer planId,
    @Bind("days") Long days, @Bind("description") String description, @Bind("issuerId") Long issuerId);

  @SqlQuery(
    "select * from credit " + 
    "where (issued_id=:workspaceId) " + 
    "   or (issuer_id=:workspaceId and issued_id is null) " +
    "order by issued_at desc"
  )
  @UseRowMapper(CreditMapper.class)
  List<Credit> findListByWorkspaceId(@Bind("workspaceId") Long workspaceId);

}
