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
import io.inprice.common.mappers.WorkspaceHistoryMapper;
import io.inprice.common.mappers.WorkspaceTransMapper;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.models.WorkspaceHistory;
import io.inprice.common.models.WorkspaceTrans;

public interface SubscriptionDao {

  @SqlQuery("select * from workspace_trans where event_id=:eventId")
  @UseRowMapper(WorkspaceTransMapper.class)
  WorkspaceTrans findByEventId(@Bind("eventId") String eventId);

  @SqlQuery("select * from workspace_trans where workspace_id=:workspaceId order by id desc")
  @UseRowMapper(WorkspaceTransMapper.class)
  List<WorkspaceTrans> findListByWorkspaceId(@Bind("workspaceId") Long workspaceId);

  @SqlUpdate(
    "insert into workspace_trans (workspace_id, event_id, successful, reason, description, file_url, event) " + 
    "values (:trans.workspaceId, :trans.eventId, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl, :event)"
  )
  boolean insertTrans(@BindBean("trans") WorkspaceTrans trans, @Bind("event") String event);

  @SqlQuery("select * from workspace_history where workspace_id=:workspaceId and status=:status order by id desc limit 1")
  @UseRowMapper(WorkspaceHistoryMapper.class)
  WorkspaceHistory findPreviousHistoryRowByStatusAndWorkspaceId(@Bind("workspaceId") Long workspaceId, @Bind("status") String status);

  @SqlUpdate(
    "update workspace " +
    "set title=:dto.title, contact_name=:dto.contactName, tax_id=:dto.taxId, tax_office=:dto.taxOffice, address_1=:dto.address1, " +
    "address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country, " +
    "plan_id=:dto.planId, pre_status=status, status=:status, subs_renewal_at=:dto.renewalDate, subs_started_at=now(), last_status_update=now() " +
    "where id=:id"
  )
  boolean startSubscription(@BindBean("dto") CustomerDTO dto, @Bind("status") String status, @Bind("linkLimit") Integer linkLimit, @Bind("id") Long id);

  @SqlUpdate("update workspace set pre_status=status, status=:status, subs_renewal_at=:renewalAt where id=:id")
  boolean renewSubscription(@Bind("id") Long id, @Bind("status") String status, @Bind("subsRenewalAt") Timestamp subsRenewalAt);

  @SqlUpdate(
    "update workspace " + 
    "set plan_id=:planId, pre_status=status, status=:status, subs_renewal_at=DATE_ADD(now(), interval <interval> day), last_status_update=now() " +
    "where id=:workspaceId"
  )
  boolean startFreeUseOrApplyVoucher(@Bind("workspaceId") Long workspaceId, @Bind("status") String status, 
    @Bind("planId") Integer planId, @Define("interval") Integer interval);

  @SqlUpdate(
    "update workspace " +
    "set subs_renewal_at=null, pre_status=status, status=:status, last_status_update=now() "+
    "where id=:id"
  )
  boolean terminate(@Bind("id") Long id, @Bind("status") WorkspaceStatus status);

  @SqlUpdate(
    "update workspace " + 
    "set plan_id=:planId, subs_started_at=now(), last_status_update=now() " +
    "where id=:workspaceId"
  )
  boolean changePlan(@Bind("workspaceId") Long workspaceId, @Bind("planId") Integer planId);

}
