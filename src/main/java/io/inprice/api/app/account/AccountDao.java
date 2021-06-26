package io.inprice.api.app.account;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.account.mapper.AccountInfo;
import io.inprice.api.app.account.mapper.AccountInfoMapper;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.common.mappers.AccountMapper;
import io.inprice.common.mappers.UserUsedMapper;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.PermType;
import io.inprice.common.models.Account;
import io.inprice.common.models.UserUsed;

public interface AccountDao {

  @SqlQuery(
		"select a.*, p.name as plan_name, p.user_limit, p.link_limit, p.alarm_limit from account as a " +
		"left join plan as p on p.id = a.plan_id " +
		"where a.id=:id"
	)
  @UseRowMapper(AccountMapper.class)
  Account findById(@Bind("id") Long id);

  @SqlQuery("select * from account where admin_id=:adminId")
  @UseRowMapper(AccountMapper.class)
  Account findByAdminId(@Bind("adminId") Long adminId);

  @SqlQuery("select * from account where name=:name and admin_id=:adminId")
  @UseRowMapper(AccountMapper.class)
  Account findByNameAndAdminId(@Bind("name") String name, @Bind("adminId") Long adminId);

  @SqlUpdate(
    "insert into account (admin_id, name, currency_code, currency_format) " + 
    "values (:adminId, :name, :currencyCode, :currencyFormat)"
  )
  @GetGeneratedKeys
  long insert(@Bind("adminId") Long adminId, @Bind("name") String name, 
    @Bind("currencyCode") String currencyCode, @Bind("currencyFormat") String currencyFormat);

  @SqlUpdate("update account set name=:name, currency_code=:currencyCode, currency_format=:currencyFormat where id=:id and admin_id=:adminId")
  boolean update(@Bind("name") String name, @Bind("currencyCode") String currencyCode, 
    @Bind("currencyFormat") String currencyFormat, @Bind("id") Long id, @Bind("adminId") Long adminId);

  @SqlUpdate(
    "update account set "+
    "title=:dto.title, address_1=:dto.address1, address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country " +
    "where id=:id"
  )
  boolean update(@BindBean("dto") CustomerDTO dto, @Bind("id") Long id);

  @SqlUpdate("update account set user_count=user_count+1 where id=:id")
  boolean increaseUserCount(@Bind("id") Long id);
  
  @SqlUpdate("update account set link_count=link_count+<count> where id=:id")
  boolean increaseLinkCount(@Bind("id") Long id, @Define("count") Integer count);
  
  @SqlUpdate("update account set alarm_count=alarm_count+1 where id=:id")
  boolean increaseAlarmCount(@Bind("id") Long id);

  @SqlUpdate("update account set alarm_count=alarm_count-1 where id=:id")
  boolean decreaseAlarmCount(@Bind("id") Long id);

  @SqlQuery(
    "select c.id, c.name, u.email from account as c " +
    "inner join user as u on u.id = c.admin_id " +
    "where c.status='SUBSCRIBED' "+
    "  and c.subs_renewal_at <= now() - interval 3 day"
  )
  @UseRowMapper(AccountInfoMapper.class)
  List<AccountInfo> findExpiredSubscriberAccountList();

  @SqlUpdate("insert into account_history (account_id, status) values (:accountId, :status)")
  boolean insertStatusHistory(@Bind("accountId") Long accountId, @Bind("status") AccountStatus status);

  @SqlUpdate(
    "insert into account_history (account_id, status, plan_id) " +
    "values (:accountId, :status, :planId)"
  )
  boolean insertStatusHistory(@Bind("accountId") Long accountId, @Bind("status") String status, @Bind("planId") Integer planId);
  
  @SqlQuery("select * from user_used where email=:email and perm_type=:permType")
  @UseRowMapper(UserUsedMapper.class)
  UserUsed hasUserUsedByEmail(@Bind("email") String email, @Bind("permType") PermType permType);

  @SqlUpdate("insert into user_used (email, perm_type) values (:email, :permType)")
  void insertUserUsed(@Bind("email") String email, @Bind("permType") PermType permType);

}
