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

public interface SessionDao {

  @SqlUpdate("delete from user_session where _hash in (<hashList>)")
  boolean deleteByHashList(@BindList("hashList") List<String> hashList);

  @SqlUpdate("delete from user_session where user_id=:userId")
  boolean deleteByUserId(@Bind("userId") Long userId);

  @SqlQuery("select * from user_session where user_id=:userId")
  @UseRowMapper(DBSessionMapper.class)
  List<ForDatabase> findListByUserId(@Bind("userId") Long userId);

  @SqlQuery("select _hash from user_session where company_id=:companyId")
  List<String> findHashesByCompanyId(@Bind("companyId") Long companyId);

  @SqlBatch(
    "insert into user_session (_hash, user_id, company_id, ip, os, browser, user_agent) " +
    "values (:hash, :userId, :companyId, :ip, :os, :browser, :userAgent)"
  )
  boolean[] insert(@BindBean List<ForDatabase> sesList);

}
