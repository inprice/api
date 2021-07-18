package io.inprice.api.app.subscription;

import java.sql.Timestamp;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.CustomerDTO;
import io.inprice.common.mappers.AccountHistoryMapper;
import io.inprice.common.mappers.AccountTransMapper;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.models.AccountHistory;
import io.inprice.common.models.AccountTrans;

public interface SubscriptionDao {

  @SqlQuery("select * from account_trans where event_id=:eventId")
  @UseRowMapper(AccountTransMapper.class)
  AccountTrans findByEventId(@Bind("eventId") String eventId);

  @SqlQuery("select * from account_trans where account_id=:accountId order by id desc")
  @UseRowMapper(AccountTransMapper.class)
  List<AccountTrans> findListByAccountId(@Bind("accountId") Long accountId);

  @SqlUpdate(
    "insert into account_trans (account_id, event_id, successful, reason, description, file_url, event) " + 
    "values (:trans.accountId, :trans.eventId, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl, :event)"
  )
  boolean insertTrans(@BindBean("trans") AccountTrans trans, @Bind("event") String event);

  @SqlQuery("select * from account_history where account_id=:accountId and status=:status order by id desc limit 1")
  @UseRowMapper(AccountHistoryMapper.class)
  AccountHistory findPreviousHistoryRowByStatusAndAccountId(@Bind("accountId") Long accountId, @Bind("status") String status);

  @SqlUpdate(
    "update account " +
    "set title=:dto.title, contact_name=:dto.contactName, address_1=:dto.address1, address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country, " +
    "plan_id=:dto.planId, pre_status=status, status=:status, subs_renewal_at=:dto.renewalDate, subs_started_at=now(), last_status_update=now() " +
    "where id=:id"
  )
  boolean startSubscription(@BindBean("dto") CustomerDTO dto, @Bind("status") String status, @Bind("linkLimit") Integer linkLimit, @Bind("id") Long id);

  @SqlUpdate("update account set pre_status=status, status=:status, subs_renewal_at=:renewalAt where id=:id")
  boolean renewSubscription(@Bind("id") Long id, @Bind("status") String status, @Bind("subsRenewalAt") Timestamp subsRenewalAt);

  @SqlUpdate(
    "update account " + 
    "set plan_id=:planId, pre_status=status, status=:status, subs_renewal_at=DATE_ADD(now(), interval <interval> day), last_status_update=now() " +
    "where id=:accountId"
  )
  boolean startFreeUseOrApplyCoupon(@Bind("accountId") Long accountId, @Bind("status") String status, 
    @Bind("planId") Integer planId, @Define("interval") Integer interval);

  @SqlUpdate(
    "update account " +
    "set subs_renewal_at=null, pre_status=status, status=:status, last_status_update=now() "+
    "where id=:id"
  )
  boolean terminate(@Bind("id") Long id, @Bind("status") AccountStatus status);

  @SqlUpdate(
    "update account " + 
    "set plan_id=:planId, subs_started_at=now(), last_status_update=now() " +
    "where id=:accountId"
  )
  boolean changePlan(@Bind("accountId") Long accountId, @Bind("planId") Integer planId);

}
