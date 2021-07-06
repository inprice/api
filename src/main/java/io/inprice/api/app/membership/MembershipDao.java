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
import io.inprice.common.mappers.MemberMapper;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Member;

public interface MembershipDao {

  final String ACCOUNT_FIELDS = 
    ", a.name as account_name, a.status as account_status, a.subs_started_at, " +
    "a.subs_renewal_at, a.last_status_update, a.plan_id, a.currency_format, a.user_count, a.link_count, a.alarm_count ";

  final String PLAN_FIELDS = ", p.name as plan_name, p.user_limit, p.link_limit, p.alarm_limit ";
  
  @SqlQuery("select * from member where id=:id and account_id=:accountId and role in ('VIEWER', 'EDITOR')")
  @UseRowMapper(MemberMapper.class)
  Member findNormalMemberById(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from member where email=:email and account_id=:accountId")
  @UseRowMapper(MemberMapper.class)
  Member findByEmail(@Bind("email") String email, @Bind("accountId") Long accountId);

  @SqlQuery(
    "select m.*" + ACCOUNT_FIELDS + PLAN_FIELDS + " from member as m " +
    "inner join account as a on a.id = m.account_id " + 
    "left join plan as p on p.id = a.plan_id " + 
    "where a.status != 'BANNED' " +
    "  and m.account_id = :accountId " + 
    "  and role in ('VIEWER', 'EDITOR') " + 
    "order by m.email"
  )
  @UseRowMapper(MemberMapper.class)
  List<Member> findNormalMemberList(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select m.*" + ACCOUNT_FIELDS + PLAN_FIELDS + " from member as m " +
    "inner join account as a on a.id = m.account_id " + 
    "left join plan as p on p.id = a.plan_id " + 
    "where a.status != 'BANNED' " +
    "  and m.email=:email " +
    "  and m.status=:status " + 
    "order by m.role, m.created_at"
  )
  @UseRowMapper(MemberMapper.class)
  List<Member> findListByEmailAndStatus(@Bind("email") String email, @Bind("status") UserStatus status);

  @SqlQuery("select * from member where email=:email and status=:status and account_id=:accountId")
  @UseRowMapper(MemberMapper.class)
  Member findByEmailAndStatus(@Bind("email") String email, @Bind("status") UserStatus status, @Bind("accountId") Long accountId);

  @SqlQuery(
    "select m.id, a.name, m.role, m.status, m.created_at from member as m " + 
    "left join account as a on a.id = m.account_id " + 
    "where a.status != 'BANNED' " +
    "  and m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.id desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMemberListByEmailAndStatus(@Bind("email") String email, @Bind("status") UserStatus status);

  @SqlQuery(
    "select user_id from member as prim " + 
    "where prim.account_id=:accountId " + 
    "  and (select count(1) from member as secon where secon.user_id=prim.user_id) <= 1"
  )
  List<Long> findUserIdListHavingJustThisAccount(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select m.id, a.name, m.role, m.status, m.updated_at from member as m " + 
    "left join account as a on a.id = m.account_id " + 
    "where a.status != 'BANNED' " +
    "  and m.email=:email " + 
    "  and m.account_id != :accountId  " + 
    "  and m.status in (<statusList>) " + 
    "order by m.status, m.updated_at desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMembershipsByEmail(@Bind("email") String email, @Bind("accountId") Long accountId, @BindList("statusList") List<String> statusList);

  @SqlUpdate(
    "insert into member (user_id, email, account_id, role, status, updated_at) " + 
    "values (:userId, :email, :accountId, :role, :status, now())"
  )
  @GetGeneratedKeys
  long insert(@Bind("userId") Long userId, @Bind("email") String email, 
    @Bind("accountId") Long accountId, @Bind("role") UserRole role, @Bind("status") UserStatus status);

  @SqlUpdate("insert into member (email, role, account_id) values (:email, :role, :accountId)")
  boolean insertInvitation(@Bind("email") String email, @Bind("role") UserRole role, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set retry=retry+1 where id=:id and retry<3 and status=:status and account_id=:accountId")
  boolean increaseSendingCount(@Bind("id") Long id, @Bind("status") UserStatus status, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set status=:status, updated_at=now() where id=:id and status!=:status and account_id=:accountId")
  boolean setStatusDeleted(@Bind("id") Long id, @Bind("status") UserStatus status, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set role=:role where id=:id and account_id=:accountId")
  boolean changeRole(@Bind("id") Long id, @Bind("role") UserRole role, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set status=:toStatus, user_id=:userId, updated_at=now() where id=:id and status=:fromStatus")
  boolean changeStatus(@Bind("id") Long id, @Bind("fromStatus") String fromStatus, @Bind("toStatus") String toStatus, @Bind("userId") Long userId);

  @SqlUpdate("update member set status=:newStatus, updated_at=now() where id=:id")
  boolean changeStatus(@Bind("id") Long id, @Bind("newStatus") String newStatus);

  @SqlUpdate("update member set pre_status=status, status='PAUSED', status_group=:statusGroup, updated_at=now() where id=:id and account_id=:accountId and status!='PAUSED")
  boolean pause(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set status=pre_status, pre_status='PAUSED', status_group=:statusGroup, updated_at=now() where id=:id and account_id=:accountId and status='PAUSED")
  boolean resume(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set user_id=:userId, status=:newStatus, updated_at=now() where email=:email and status=:oldStatus and account_id=:accountId")
  boolean activate(@Bind("userId") Long userId, @Bind("oldStatus") UserStatus oldStatus, 
    @Bind("newStatus") UserStatus newStatus, @Bind("email") String email, @Bind("accountId") Long accountId);

}
