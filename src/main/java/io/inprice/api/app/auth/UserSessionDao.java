package io.inprice.api.app.auth;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.auth.mapper.DBSessionMapper;
import io.inprice.api.session.info.ForDatabase;

public interface UserSessionDao {

  @SqlUpdate("delete from user_session where _hash in (<hashList>)")
  boolean deleteByHashList(@BindList("hashList") List<String> hashList);

  @SqlUpdate("delete from user_session where user_id=:userId")
  boolean deleteByUserId(@Bind("userId") Long userId);

  @SqlUpdate("update user_session set accessed_at = now() where _hash=:hash")
  boolean refreshAccessedAt(@Bind("hash") String hash);

  @SqlQuery("select * from user_session where user_id=:userId")
  @UseRowMapper(DBSessionMapper.class)
  List<ForDatabase> findListByUserId(@Bind("userId") Long userId);
  
  @SqlQuery("select _hash from user_session where workspace_id=:workspaceId")
  List<String> findHashesByWorkspaceId(@Bind("workspaceId") Long workspaceId);

  @SqlQuery("select _hash from user_session where user_id=:userId")
  List<String> findHashesByUserId(@Bind("userId") Long userId);

  @SqlBatch(
    "insert into user_session (_hash, user_id, workspace_id, ip, user_agent) " +
    "values (:ses.hash, :ses.userId, :ses.workspaceId, :ses.ip, :ses.userAgent)"
  )
  void insertBulk(@BindBean("ses") List<ForDatabase> sesList);

  @SqlQuery("select distinct ip, user_agent, accessed_at from user_session where user_id=:userId order by accessed_at desc limit 25")
  @UseRowMapper(DBSessionMapper.class)
  List<ForDatabase> findOpenedSessions(@Bind("userId") Long userId);

}
