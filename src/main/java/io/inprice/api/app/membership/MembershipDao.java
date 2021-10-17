package io.inprice.api.app.membership;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.membership.mapper.ActiveMember;
import io.inprice.api.app.membership.mapper.ActiveMemberMapper;
import io.inprice.common.mappers.MembershipMapper;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Membership;

public interface MembershipDao {

  final String WORKSPACE_FIELDS = 
    ", w.name as workspace_name, w.status as workspace_status, w.subs_started_at, " +
    "w.subs_renewal_at, w.last_status_update, w.plan_id, w.currency_code, w.currency_format, w.user_count, w.link_count, w.alarm_count ";

  final String PLAN_FIELDS = ", p.name as plan_name, p.user_limit, p.link_limit, p.alarm_limit ";
  
  @SqlQuery("select * from membership where id=:id and workspace_id=:workspaceId and role in ('VIEWER', 'EDITOR')")
  @UseRowMapper(MembershipMapper.class)
  Membership findNormalMemberById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from membership where email=:email and workspace_id=:workspaceId")
  @UseRowMapper(MembershipMapper.class)
  Membership findByEmail(@Bind("email") String email, @Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select m.*" + WORKSPACE_FIELDS + PLAN_FIELDS + " from membership as m " +
    "inner join workspace as w on w.id = m.workspace_id " + 
    "left join plan as p on p.id = w.plan_id " + 
    "where w.status != 'BANNED' " +
    "  and m.workspace_id = :workspaceId " + 
    "  and role in ('VIEWER', 'EDITOR') " + 
    "order by m.email"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> findNormalMemberList(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select m.*" + WORKSPACE_FIELDS + PLAN_FIELDS + " from membership as m " +
    "inner join workspace as w on w.id = m.workspace_id " + 
    "left join plan as p on p.id = w.plan_id " + 
    "where w.status != 'BANNED' " +
    "  and m.email=:email " +
    "  and m.status=:status " + 
    "order by w.name, m.role"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> findListByEmailAndStatus(@Bind("email") String email, @Bind("status") UserStatus status);

  @SqlQuery("select * from membership where email=:email and status=:status and workspace_id=:workspaceId")
  @UseRowMapper(MembershipMapper.class)
  Membership findByEmailAndStatus(@Bind("email") String email, @Bind("status") UserStatus status, @Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select m.id, w.name, m.role, m.status, m.created_at from membership as m " + 
    "left join workspace as w on w.id = m.workspace_id " + 
    "where w.status != 'BANNED' " +
    "  and m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.id desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMemberListByEmailAndStatus(@Bind("email") String email, @Bind("status") UserStatus status);

  @SqlQuery(
    "select user_id from membership as prim " + 
    "where prim.workspace_id=:workspaceId " + 
    "  and (select count(1) from membership as secon where secon.user_id=prim.user_id) <= 1"
  )
  List<Long> findUserIdListHavingJustThisWorkspace(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select m.id, w.name, m.role, m.status, m.updated_at from membership as m " + 
    "left join workspace as w on w.id = m.workspace_id " + 
    "where w.status != 'BANNED' " +
    "  and m.email=:email " + 
    "  and m.status in (<statusList>) " + 
    "order by w.name, m.status"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMembershipsByEmail(@Bind("email") String email, @BindList("statusList") List<String> statusList);

  @SqlUpdate(
    "insert into membership (user_id, email, workspace_id, role, status, updated_at) " + 
    "values (:userId, :email, :workspaceId, :role, :status, now())"
  )
  @GetGeneratedKeys
  long insert(@Bind("userId") Long userId, @Bind("email") String email, 
    @Bind("workspaceId") Long workspaceId, @Bind("role") UserRole role, @Bind("status") UserStatus status);

  @SqlUpdate("insert into membership (email, role, workspace_id) values (:email, :role, :workspaceId)")
  boolean insertInvitation(@Bind("email") String email, @Bind("role") UserRole role, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update membership set retry=retry+1 where id=:id and retry<3 and status=:status and workspace_id=:workspaceId")
  boolean increaseSendingCount(@Bind("id") Long id, @Bind("status") UserStatus status, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update membership set status=:status, updated_at=now() where id=:id and status!=:status and workspace_id=:workspaceId")
  boolean setStatusDeleted(@Bind("id") Long id, @Bind("status") UserStatus status, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update membership set role=:role where id=:id and workspace_id=:workspaceId")
  boolean changeRole(@Bind("id") Long id, @Bind("role") UserRole role, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate(
		"update membership " +
		"set status=:toStatus, updated_at=now() " +
		"where id=:id " +
		"  and user_id=:userId " +
		"  and status in (<fromStatuses>)"
	)
  boolean changeStatus(@Bind("id") Long id, @BindList("fromStatuses") List<UserStatus> fromStatuses, 
  		@Bind("toStatus") UserStatus toStatus, @Bind("userId") Long userId);

  @SqlUpdate("update membership set pre_status=status, status='PAUSED', updated_at=now() where id=:id and workspace_id=:workspaceId and status!='PAUSED'")
  boolean pause(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update membership set status=pre_status, pre_status='PAUSED', updated_at=now() where id=:id and workspace_id=:workspaceId and status='PAUSED'")
  boolean resume(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update membership set user_id=:userId, status=:newStatus, updated_at=now() where email=:email and status=:oldStatus and workspace_id=:workspaceId")
  boolean activate(@Bind("userId") Long userId, @Bind("oldStatus") UserStatus oldStatus, 
    @Bind("newStatus") UserStatus newStatus, @Bind("email") String email, @Bind("workspaceId") Long workspaceId);

}
