package io.inprice.api.app.lookup.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class LookupWithInfoMapper implements RowMapper<LookupWithInfo> {

  @Override
  public LookupWithInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
    LookupWithInfo m = new LookupWithInfo();

    m.setId(rs.getLong("id"));
    m.setName(rs.getString("name"));
    m.setCount(rs.getInt("counter"));

    return m;
  }

}