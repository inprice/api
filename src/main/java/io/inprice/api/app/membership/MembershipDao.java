package io.inprice.api.app.membership;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.membership.mapper.ActiveMembership;
import io.inprice.api.app.membership.mapper.ActiveMembershipMapper;
import io.inprice.common.mappers.MembershipMapper;
import io.inprice.common.models.Membership;

public interface MembershipDao {

  @SqlQuery("select * from membership where id=:id")
  @UseRowMapper(MembershipMapper.class)
  Membership findById(@Bind("id") Long id);

  @SqlQuery("select * from membership where email=:email and company_id=:companyId")
  @UseRowMapper(MembershipMapper.class)
  Membership findByEmail(@Bind("email") String email, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select m.*, c.currency_format, c.name as company_name, c.plan_id from membership as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email != :email " + 
    "  and company_id = :companyId " + 
    "order by m.email"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> findListByNotEmail(@Bind("email") String email, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select m.*, c.currency_format, c.name as company_name, c.plan_id, c.subs_status, c.subs_renewal_at from membership as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.role, m.created_at"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> findListByEmailAndStatus(@Bind("email") String email, @Bind("status") String status);

  @SqlQuery("select * from membership where email=:email and status=:status and company_id=:companyId")
  @UseRowMapper(MembershipMapper.class)
  Membership findListByEmailAndStatusAndCompanyId(@Bind("email") String email, 
    @Bind("status") String status, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select mem.id, c.name, mem.role, mem.created_at from membership as mem " + 
    "left join company as c on c.id = mem.company_id " + 
    "where email=:email " + 
    "  and mem.status:status " + 
    "order by mem.created_at desc"
  )
  @UseRowMapper(ActiveMembershipMapper.class)
  List<ActiveMembership> findMembershipListByEmailAndStatus(@Bind("email") String email, @Bind("status") String status);

  @SqlQuery(
    "select mem.id, c.name, mem.role, mem.updated_at from membership as mem " + 
    "left join company as c on c.id = mem.company_id " + 
    "where email=:email " + 
    "  and company_id!=:company_id  " + 
    "  and mem.status in (<statusList>) " + 
    "order by mem.status, mem.updated_at desc"
  )
  @UseRowMapper(ActiveMembershipMapper.class)
  List<ActiveMembership> findMembershipListByEmailAndStatusListButNotCompanyId(@Bind("email") String email, 
    @Bind("companyId") Long companyId, @BindList("statusList") List<String> statusList);

  @SqlUpdate(
    "insert into membership (user_id, email, company_id, role, status, updated_at) " + 
    "values (:userId, :email, :companyId, :role, :status, now())"
  )
  @GetGeneratedKeys("id")
  long insert(@Bind("userId") Long userId, @Bind("email") String email, 
    @Bind("companyId") Long companyId, @Bind("role") String role, @Bind("status") String status);

  @SqlUpdate("insert into membership (email, role, company_id) values (:email, :role, :companyId)")
  boolean insertInvitation(@Bind("email") String email, @Bind("role") String role, @Bind("companyId") Long companyId);

  @SqlUpdate("update membership set retry=retry+1 where id=:id and retry<3 and status=:status and company_id=:companyId")
  boolean increaseSendingCount(@Bind("id") Long id, @Bind("status") String status, @Bind("companyId") Long companyId);

  @SqlUpdate("update membership set status=?, updated_at=now() where id=:id and status != :status and company_id=:companyId")
  boolean setStatusDeleted(@Bind("id") Long id, @Bind("status") String status, @Bind("companyId") Long companyId);

  @SqlUpdate("update membership set role=:role where id=:id and company_id=:companyId")
  boolean changeRole(@Bind("id") Long id, @Bind("role") String role, @Bind("companyId") Long companyId);

  @SqlUpdate("update membership set status=:toStatus, user_id=:userId, updated_at=now() where id=:id and status=:fromStatus")
  boolean changeStatus(@Bind("id") Long id, @Bind("fromStatus") String fromStatus, @Bind("toStatus") String toStatus, @Bind("userId") Long userId);

  @SqlUpdate("update membership set status=:newStatus, updated_at=now() where id=:id")
  boolean changeStatus(@Bind("id") Long id, @Bind("newStatus") String newStatus);

  @SqlUpdate("update membership set pre_status=status, status='PAUSED', updated_at=now() where id=:id and company_id=:companyId and status!='PAUSED")
  boolean pause(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlUpdate("update membership set status=pre_status, pre_status='PAUSED', updated_at=now() where id=:id and company_id=:companyId and status='PAUSED")
  boolean resume(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlUpdate("update membership set user_id=:userId, status=:newStatus, updated_at=now() where email=:email and status=:oldStatus and company_id=:companyId")
  boolean activate(@Bind("userId") Long userId, @Bind("oldStatus") String oldStatus, 
    @Bind("newStatus") String newStatus, @Bind("email") String email, @Bind("companyId") Long companyId);

}
