package io.inprice.api.app.superuser.account;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AccountHistoryMapper;
import io.inprice.common.mappers.AccountMapper;
import io.inprice.common.mappers.AccountTransMapper;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.MemberMapper;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountHistory;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.Member;

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
		"select a.*, a.id as xid, p.name as plan_name, p.user_limit, p.link_limit, p.alarm_limit from account as a "+
		"left join plan as p on p.id = a.plan_id " +
		"where a.id=:id"
	)
  @UseRowMapper(AccountMapper.class)
	Account findById(@Bind("id") Long id);

  @SqlQuery(
		"select m.*, a.name as account_name, a.status as account_status from member m " +
		"inner join account a on a.id = m.account_id "+
		"where account_id=:account_id " +
		"order by role, created_at"
	)
  @UseRowMapper(MemberMapper.class)
  List<Member> fetchMemberList(@Bind("account_id") Long accountId);

  @SqlQuery(
		"select *, p.name as plan_name from account_history h "+
		"left join plan as p on p.id = h.plan_id " +
		"where h.account_id=:accountId "+
		"order by h.created_at desc"
	)
  @UseRowMapper(AccountHistoryMapper.class)
  List<AccountHistory> fetchHistory(@Bind("accountId") Long accountId);

  @SqlQuery("select * from account_trans where account_id=:accountId order by created_at desc")
  @UseRowMapper(AccountTransMapper.class)
  List<AccountTrans> fetchTransactionList(@Bind("accountId") Long accountId);

	@SqlQuery(
		"select id, name from user " +
		"where id in (select user_id from member where account_id=:accountId) " +
		"order by name"
	)
  @UseRowMapper(IdNamePairMapper.class)
  List<Pair<Long, String>> fetchUserListByAccountId(@Bind("accountId") Long accountId);

}
