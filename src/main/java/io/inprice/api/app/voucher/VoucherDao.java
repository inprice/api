package io.inprice.api.app.voucher;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.VoucherMapper;
import io.inprice.common.models.Voucher;

public interface VoucherDao {

  @SqlQuery("select * from voucher where code=:code")
  @UseRowMapper(VoucherMapper.class)
  Voucher findByCode(@Bind("code") String code);

  @SqlUpdate("update voucher set issued_id=:issuedId, issued_at=now() where code=:code")
  boolean applyFor(@Bind("code") String code, @Bind("issuedId") Long issuedId);

  @SqlUpdate(
    "insert into voucher (code, plan_id, days, description, issuer_id) " +
    "values (:code, :planId, :days, :description, :issuerId)"
  )
  boolean create(@Bind("code") String code, @Bind("planId") Integer planId,
    @Bind("days") Long days, @Bind("description") String description, @Bind("issuerId") Long issuerId);

  @SqlQuery(
    "select * from voucher " + 
    "where (issued_id=:workspaceId) " + 
    "   or (issuer_id=:workspaceId and issued_id is null) " +
    "order by issued_at desc"
  )
  @UseRowMapper(VoucherMapper.class)
  List<Voucher> findListByWorkspaceId(@Bind("workspaceId") Long workspaceId);

}
