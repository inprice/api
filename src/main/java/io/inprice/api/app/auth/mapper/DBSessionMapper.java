package io.inprice.api.app.auth.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.api.session.info.ForDatabase;
import io.inprice.common.mappers.Helper;

public class DBSessionMapper implements RowMapper<ForDatabase> {

  @Override
  public ForDatabase map(ResultSet rs, StatementContext ctx) throws SQLException {
    ForDatabase m = new ForDatabase();

    if (Helper.hasColumn(rs, "_hash")) m.setHash(rs.getString("_hash"));
    if (Helper.hasColumn(rs, "user_id")) m.setUserId(Helper.nullLongHandler(rs, "user_id"));
    if (Helper.hasColumn(rs, "workspace_id")) m.setWorkspaceId(Helper.nullLongHandler(rs, "workspace_id"));
    if (Helper.hasColumn(rs, "ip")) m.setIp(rs.getString("ip"));
    if (Helper.hasColumn(rs, "os")) m.setOs(rs.getString("os"));
    if (Helper.hasColumn(rs, "browser")) m.setBrowser(rs.getString("browser"));
    if (Helper.hasColumn(rs, "user_agent")) m.setUserAgent(rs.getString("user_agent"));
    if (Helper.hasColumn(rs, "accessed_at")) m.setAccessedAt(rs.getTimestamp("accessed_at"));
    
    //transient
    if (Helper.hasColumn(rs, "workspace_name")) m.setWorkspaceName(rs.getString("workspace_name"));

    return m;
  }

}