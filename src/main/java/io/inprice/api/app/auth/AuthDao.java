package io.inprice.api.app.auth;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.auth.dto.UserDTO;
import io.inprice.api.mapper.DBSessionMapper;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.common.mappers.MembershipMapper;
import io.inprice.common.mappers.UserMapper;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;

interface AuthDao {

  @SqlUpdate("delete from user_session where _hash in (<hashList>)")
  boolean deleteSessionByHashList(@BindList List<String> hashList);

  @SqlUpdate("delete from user_session where user_id=:userId")
  boolean deleteSessionByUserId(@BindList Long userId);

  @SqlQuery("select * from user_session where user_id=:userId")
  @UseRowMapper(DBSessionMapper.class)
  List<ForDatabase> getUserSessions(@Bind Long userId);

  @SqlBatch(
    "insert into user_session (_hash, user_id, company_id, ip, os, browser, user_agent) " +
    "values (:ses.hash, :ses.userId, :ses.companyId, :ses.ip, :ses.os, :ses.browser, :ses.userAgent)"
  )
  boolean[] addSessions(@BindBean("ses") List<ForDatabase> sesList);

  @SqlQuery(
    "select m.*, c.currency_format, c.name as company_name, c.plan_id, c.subs_status, c.subs_renewal_at from membership as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.role, m.created_at"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> getUserMemberships(@Bind String email, @Bind String status);

  @SqlQuery("select * from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findUserByEmail(@Bind String email);

  @SqlQuery("select * from membership where email=:email and status=:status and company_id=:companyId")
  @UseRowMapper(MembershipMapper.class)
  Membership findMembershipdByEmailAndStatus(@Bind String email, @Bind String status, @Bind Long companyId);

  @SqlBatch(
    "insert into user (email, name, timezone, password_salt, password_hash) " +
    "values (:user.email, :user.name, :user.timezone, :passwordSalt, :passwordHash)"
  )
  @GetGeneratedKeys("id")
  long insertUser(@BindBean("user") UserDTO userDto, @Bind String passwordSalt, @Bind String passwordHash);

  @SqlUpdate("update user set password_salt=:passwordSalt, password_hash=:passwordHash where id=:id")
  boolean updateUserPassword(@Bind Long id, @Bind String passwordSalt, @Bind String passwordHash);

  @SqlUpdate("update membership set user_id=:userId, status=:newStatus, updated_at=now() where email=:email and status=:oldStatus and company_id=:companyId")
  boolean activateMembership(@Bind Long userId, @Bind String newStatus, @Bind String email, @Bind String oldStatus, @Bind Long companyId);

}
