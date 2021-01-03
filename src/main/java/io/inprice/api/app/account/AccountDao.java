package io.inprice.api.app.account;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.account.mapper.AccountInfo;
import io.inprice.api.app.account.mapper.AccountInfoMapper;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.common.mappers.AccountMapper;
import io.inprice.common.models.Account;

public interface AccountDao {

  @SqlQuery("select * from account where id=:id")
  @UseRowMapper(AccountMapper.class)
  Account findById(@Bind("id") Long id);

  @SqlQuery("select * from account where admin_id=:adminId")
  @UseRowMapper(AccountMapper.class)
  Account findByAdminId(@Bind("adminId") Long adminId);

  @SqlQuery("select * from account where name=:name and admin_id=:adminId")
  @UseRowMapper(AccountMapper.class)
  Account findByNameAndAdminId(@Bind("name") String name, @Bind("adminId") Long adminId);

  @SqlQuery("select * from account where cust_id=:custId")
  @UseRowMapper(AccountMapper.class)
  Account findByCustId(@Bind("custId") String custId);

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
  
  @SqlUpdate("update account set product_count=product_count+1 where id=:id and product_count<product_limit")
  boolean increaseProductCountById(@Bind("id") Long id);

  // only two days remaining (last op. is to sending a final message)
  @SqlQuery(
    "select * from account "+
    "where status in (<statusList>) "+
    "  and TIMESTAMPDIFF(DAY, renewal_at, now()) > 1 "+
    "  and TIMESTAMPDIFF(DAY, renewal_at, now()) < 4"
  )
  @UseRowMapper(AccountMapper.class)
  List<Account> findAboutToExpiredFreeAccountList(@BindList("statusList") List<String> statusList);

  @SqlQuery("select * from account where status in (<statusList>) and renewal_at <= now()")
  @UseRowMapper(AccountMapper.class)
  List<Account> findExpiredFreeAccountList(@BindList("statusList") List<String> statusList);

  @SqlQuery(
    "select c.id, c.name, u.email, c.cust_id from account as c " +
    "inner join user as u on u.id = c.admin_id " +
    "where c.status='SUBSCRIBED' "+
    "  and c.renewal_at <= now() - interval 3 day"
  )
  @UseRowMapper(AccountInfoMapper.class)
  List<AccountInfo> findExpiredSubscriberAccountList();

  @SqlUpdate("insert into account_history (account_id, status) values (:accountId, :status)")
  boolean insertStatusHistory(@Bind("accountId") Long accountId, @Bind("status") String status);

  @SqlUpdate(
    "insert into account_history (account_id, status, plan_name, subs_id, cust_id) " +
    "values (:accountId, :status, :planName, :subsId, :custId)"
  )
  boolean insertStatusHistory(@Bind("accountId") Long accountId, @Bind("status") String status, 
    @Bind("planName") String planName, @Bind("subsId") String subsId, @Bind("custId") String custId);

}