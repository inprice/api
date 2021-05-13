package io.inprice.api.app.member;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.member.mapper.ActiveMember;
import io.inprice.api.app.member.mapper.ActiveMemberMapper;
import io.inprice.common.mappers.MemberMapper;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Member;

public interface MemberDao {

  final String ACCOUNT_FIELDS = 
    ", a.name as account_name, a.status as account_status, a.subs_started_at, " +
    "a.subs_renewal_at, a.last_status_update, a.plan_id, a.currency_format, a.user_count, a.link_count, a.alarm_count ";

  final String PLAN_FIELDS = ", p.name as plan_name, p.link_limit, p.alarm_limit ";
  
  @SqlQuery("select * from member where id=:id")
  @UseRowMapper(MemberMapper.class)
  Member findById(@Bind("id") Long id);

  @SqlQuery("select * from member where email=:email and account_id=:accountId")
  @UseRowMapper(MemberMapper.class)
  Member findByEmail(@Bind("email") String email, @Bind("accountId") Long accountId);

  @SqlQuery(
    "select m.*" + ACCOUNT_FIELDS + PLAN_FIELDS + " from member as m " +
    "inner join account as a on a.id = m.account_id " + 
    "left join plan as p on p.id = a.plan_id " + 
    "where m.email != :email " + 
    "  and m.account_id = :accountId " + 
    "order by m.email"
  )
  @UseRowMapper(MemberMapper.class)
  List<Member> findListByNotEmail(@Bind("email") String email, @Bind("accountId") Long accountId);

  @SqlQuery(
    "select m.*" + ACCOUNT_FIELDS + PLAN_FIELDS + " from member as m " +
    "inner join account as a on a.id = m.account_id " + 
    "left join plan as p on p.id = a.plan_id " + 
    "where m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.role, m.created_at"
  )
  @UseRowMapper(MemberMapper.class)
  List<Member> findListByEmailAndStatus(@Bind("email") String email, @Bind("status") String status);

  @SqlQuery("select * from member where email=:email and status=:status and account_id=:accountId")
  @UseRowMapper(MemberMapper.class)
  Member findByEmailAndStatus(@Bind("email") String email, @Bind("status") String status, @Bind("accountId") Long accountId);

  @SqlQuery(
    "select mem.id, c.name, mem.role, mem.status, mem.created_at from member as mem " + 
    "left join account as c on c.id = mem.account_id " + 
    "where email=:email " + 
    "  and mem.status=:status " + 
    "order by mem.id desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMemberListByEmailAndStatus(@Bind("email") String email, @Bind("status") String status);

  @SqlQuery(
    "select user_id from member as prim " + 
    "where prim.account_id=:accountId " + 
    "  and (select count(1) from member as secon where secon.user_id=prim.user_id) <= 1"
  )
  List<Long> findUserIdListHavingJustThisAccount(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select mem.id, c.name, mem.role, mem.status, mem.updated_at from member as mem " + 
    "left join account as c on c.id = mem.account_id " + 
    "where email=:email " + 
    "  and account_id!=:accountId  " + 
    "  and mem.status in (<statusList>) " + 
    "order by mem.status, mem.updated_at desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMembershipsByEmail(@Bind("email") String email, @Bind("accountId") Long accountId, @BindList("statusList") List<String> statusList);

  @SqlUpdate(
    "insert into member (user_id, email, account_id, role, status, updated_at) " + 
    "values (:userId, :email, :accountId, :role, :status, now())"
  )
  @GetGeneratedKeys
  long insert(@Bind("userId") Long userId, @Bind("email") String email, 
    @Bind("accountId") Long accountId, @Bind("role") String role, @Bind("status") String status);

  @SqlUpdate("insert into member (email, role, account_id) values (:email, :role, :accountId)")
  boolean insertInvitation(@Bind("email") String email, @Bind("role") String role, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set retry=retry+1 where id=:id and retry<3 and status=:status and account_id=:accountId")
  boolean increaseSendingCount(@Bind("id") Long id, @Bind("status") String status, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set status=:status, updated_at=now() where id=:id and status!=:status and account_id=:accountId")
  boolean setStatusDeleted(@Bind("id") Long id, @Bind("status") String status, @Bind("accountId") Long accountId);

  @SqlUpdate("update member set role=:role where id=:id and account_id=:accountId")
  boolean changeRole(@Bind("id") Long id, @Bind("role") String role, @Bind("accountId") Long accountId);

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
