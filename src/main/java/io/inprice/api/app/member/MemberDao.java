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
import io.inprice.common.models.Member;

public interface MemberDao {

  final String COMPANY_FIELDS = 
    ", c.name as company_name, c.status as company_status, c.cust_id, c.subs_started_at, " +
    "c.last_status_update, c.plan_name, c.renewal_at, c.currency_format, c.product_count ";

  @SqlQuery("select * from member where id=:id")
  @UseRowMapper(MemberMapper.class)
  Member findById(@Bind("id") Long id);

  @SqlQuery("select * from member where email=:email and company_id=:companyId")
  @UseRowMapper(MemberMapper.class)
  Member findByEmail(@Bind("email") String email, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select m.*" + COMPANY_FIELDS + " from member as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email != :email " + 
    "  and company_id = :companyId " + 
    "order by m.email"
  )
  @UseRowMapper(MemberMapper.class)
  List<Member> findListByNotEmail(@Bind("email") String email, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select m.*" + COMPANY_FIELDS + " from member as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.role, m.created_at"
  )
  @UseRowMapper(MemberMapper.class)
  List<Member> findListByEmailAndStatus(@Bind("email") String email, @Bind("status") String status);

  @SqlQuery("select * from member where email=:email and status=:status and company_id=:companyId")
  @UseRowMapper(MemberMapper.class)
  Member findByEmailAndStatus(@Bind("email") String email, @Bind("status") String status, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select mem.id, c.name, mem.role, mem.status, mem.created_at from member as mem " + 
    "left join company as c on c.id = mem.company_id " + 
    "where email=:email " + 
    "  and mem.status=:status " + 
    "order by mem.id desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMemberListByEmailAndStatus(@Bind("email") String email, @Bind("status") String status);

  @SqlQuery(
    "select user_id from member as prim " + 
    "where prim.company_id=:companyId " + 
    "  and (select count(1) from member as secon where secon.user_id=prim.user_id) <= 1"
  )
  List<Long> findUserIdListHavingJustThisCompany(@Bind("companyId") Long companyId);

  @SqlQuery(
    "select mem.id, c.name, mem.role, mem.status, mem.updated_at from member as mem " + 
    "left join company as c on c.id = mem.company_id " + 
    "where email=:email " + 
    "  and company_id!=:companyId  " + 
    "  and mem.status in (<statusList>) " + 
    "order by mem.status, mem.updated_at desc"
  )
  @UseRowMapper(ActiveMemberMapper.class)
  List<ActiveMember> findMembershipsByEmail(@Bind("email") String email, @Bind("companyId") Long companyId, @BindList("statusList") List<String> statusList);

  @SqlUpdate(
    "insert into member (user_id, email, company_id, role, status, updated_at) " + 
    "values (:userId, :email, :companyId, :role, :status, now())"
  )
  @GetGeneratedKeys
  long insert(@Bind("userId") Long userId, @Bind("email") String email, 
    @Bind("companyId") Long companyId, @Bind("role") String role, @Bind("status") String status);

  @SqlUpdate("insert into member (email, role, company_id) values (:email, :role, :companyId)")
  boolean insertInvitation(@Bind("email") String email, @Bind("role") String role, @Bind("companyId") Long companyId);

  @SqlUpdate("update member set retry=retry+1 where id=:id and retry<3 and status=:status and company_id=:companyId")
  boolean increaseSendingCount(@Bind("id") Long id, @Bind("status") String status, @Bind("companyId") Long companyId);

  @SqlUpdate("update member set status=:status, updated_at=now() where id=:id and status!=:status and company_id=:companyId")
  boolean setStatusDeleted(@Bind("id") Long id, @Bind("status") String status, @Bind("companyId") Long companyId);

  @SqlUpdate("update member set role=:role where id=:id and company_id=:companyId")
  boolean changeRole(@Bind("id") Long id, @Bind("role") String role, @Bind("companyId") Long companyId);

  @SqlUpdate("update member set status=:toStatus, user_id=:userId, updated_at=now() where id=:id and status=:fromStatus")
  boolean changeStatus(@Bind("id") Long id, @Bind("fromStatus") String fromStatus, @Bind("toStatus") String toStatus, @Bind("userId") Long userId);

  @SqlUpdate("update member set status=:newStatus, updated_at=now() where id=:id")
  boolean changeStatus(@Bind("id") Long id, @Bind("newStatus") String newStatus);

  @SqlUpdate("update member set pre_status=status, status='PAUSED', updated_at=now() where id=:id and company_id=:companyId and status!='PAUSED")
  boolean pause(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlUpdate("update member set status=pre_status, pre_status='PAUSED', updated_at=now() where id=:id and company_id=:companyId and status='PAUSED")
  boolean resume(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlUpdate("update member set user_id=:userId, status=:newStatus, updated_at=now() where email=:email and status=:oldStatus and company_id=:companyId")
  boolean activate(@Bind("userId") Long userId, @Bind("oldStatus") String oldStatus, 
    @Bind("newStatus") String newStatus, @Bind("email") String email, @Bind("companyId") Long companyId);

}
