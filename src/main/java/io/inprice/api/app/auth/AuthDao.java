package io.inprice.api.app.auth;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

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

  @SqlQuery("select * from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findUserByEmail(@Bind String email);

  @SqlUpdate("update user set password_salt=:salt, password_hash=:hash where id=:id")
  boolean updateUserPassword(@Bind Long id, @Bind String salt, @Bind String hash);

  @SqlQuery(
    "select m.*, c.currency_format, c.name as company_name, c.plan_id, c.subs_status, c.subs_renewal_at from membership as m " +
    "inner join company as c on c.id = m.company_id " + 
    "where m.email=:email " + 
    "  and m.status=:status " + 
    "order by m.role, m.created_at"
  )
  @UseRowMapper(MembershipMapper.class)
  List<Membership> getUserMemberships(@Bind String email, @Bind String status);

  //@SqlUpdate("delete from user_session where user_id=:userId and company_id=:companyId")
  //boolean deleteSessionByUserIdAndCompanyId(@BindList Long userId, @Bind Long companyId);

  //@SqlQuery("select * from user_session where user_id=:userId and company_id=:companyId")
  //@UseRowMapper(DBSessionMapper.class)
  //List<ForDatabase> getUserSessions(@Bind Long userId, @Bind Long companyId);

  //TODO: accessguard tarafına alınmalı
  //@SqlUpdate("update user_session set accessed_at = now() where _hash=:hash")
  //boolean refreshAccessedAt(@Bind String hash);

  //TODO: userdao tarafına alınmalı
  //@SqlQuery("select distinct os, browser, ip, accessed_at from user_session where user_id=:userId and _hash not in (<hashList>)")
  //@UseRowMapper(DBSessionMapper.class)
  //List<ForDatabase> getOpenedSessions(@Bind Long userId, @BindList List<String> hashList);

}
