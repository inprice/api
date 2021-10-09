package io.inprice.api.app.workspace.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class WorkspaceInfoMapper implements RowMapper<WorkspaceInfo> {

  @Override
  public WorkspaceInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
    WorkspaceInfo m = new WorkspaceInfo();

    if (Helper.hasColumn(rs, "id")) m.setId(Helper.nullLongHandler(rs, "id"));
    if (Helper.hasColumn(rs, "name")) m.setEmail(rs.getString("name"));
    if (Helper.hasColumn(rs, "email")) m.setEmail(rs.getString("email"));

    return m;
  }

}