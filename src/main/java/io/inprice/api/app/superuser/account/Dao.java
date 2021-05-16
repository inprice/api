package io.inprice.api.app.superuser.account;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.common.mappers.AccountMapper;
import io.inprice.common.models.Account;

public interface Dao {

  @SqlQuery(
		"select a.id as xid, a.name, u.email, currency_code, country from account as a " +
		"inner join user as u on u.id = a.admin_id " +
		"where a.name like :dto.term " +
		"order by a.name " +
		"limit :dto.rowCount, :dto.rowLimit "
	)
  @UseRowMapper(AccountMapper.class)
	List<Account> search(@BindBean("dto") BaseSearchDTO dto);
	
	@SqlQuery(
		"select a.*, p.name as plan_name from account as a "+
		"left join plan as p on p.id = a.plan_id " +
		"where a.id=:id"
	)
  @UseRowMapper(AccountMapper.class)
	Account findById(@Bind("id") Long id);
	
	@SqlUpdate("update user set banned=true, ban_reason=:reason banned_at=now() where id=:id")
	boolean ban(@Bind("id") Long id, @Bind("reason") String reason);

	@SqlUpdate("update user set banned=false, ban_reason=null banned_at=null where id=:id")
	boolean revokeBan(@Bind("id") Long id);

}
