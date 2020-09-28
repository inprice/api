package io.inprice.api.app.membership;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.MembershipMapper;
import io.inprice.common.models.Membership;

interface MembershipDao {

  @SqlQuery("select * from membership where id=:id")
  @UseRowMapper(MembershipMapper.class)
  Membership findById(@Bind Long id);

  @SqlQuery("select * from membership where email=:email and company_id=:companyId")
  @UseRowMapper(MembershipMapper.class)
  Membership findByEmail(@Bind String email, @Bind Long companyId);

  @SqlQuery(
    "select m.*, c.currency_format, c.name as company_name, c.plan_id from membership as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email != :email " + 
    "  and company_id = :companyId " + 
    "order by m.email"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> findListByNotEmail(@Bind String email, @Bind Long companyId);

  @SqlUpdate("insert into membership (email, role, company_id) values (:email, :role, :companyId)")
  boolean insertInvitation(@Bind String email, @Bind String role, @Bind Long companyId);

  @SqlUpdate("update membership set retry=retry+1 where id=:id and retry<3 and status=:status and company_id=:companyId")
  boolean increaseSendingCount(@Bind Long id, @Bind String status, @Bind Long companyId);

  @SqlUpdate("update membership set status=?, updated_at=now() where id=:id and status != :status and company_id=:companyId")
  boolean setStatusDeleted(@Bind Long id, @Bind String status, @Bind Long companyId);

  @SqlUpdate("update membership set role=:role where id=:id and company_id=:companyId")
  boolean changeRole(@Bind Long id, @Bind String role, @Bind Long companyId);

  @SqlUpdate("update membership set pre_status=status, status='PAUSED', updated_at=now() where id=:id and company_id=:companyId and status!='PAUSED")
  boolean pause(@Bind Long id, @Bind Long companyId);

  @SqlUpdate("update membership set status=pre_status, pre_status='PAUSED', updated_at=now() where id=:id and company_id=:companyId and status='PAUSED")
  boolean resume(@Bind Long id, @Bind Long companyId);

  @SqlQuery("select name from user where email=:email")
  String findUserNameByEmail(@Bind String email);

}
