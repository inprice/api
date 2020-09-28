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
