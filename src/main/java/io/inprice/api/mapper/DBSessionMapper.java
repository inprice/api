package io.inprice.api.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.api.session.info.ForDatabase;

public class DBSessionMapper implements RowMapper<ForDatabase> {

  @Override
  public ForDatabase map(ResultSet rs, StatementContext ctx) throws SQLException {
    return new ForDatabase(
      rs.getString("_hash"),
      rs.getLong("user_id"),
      rs.getLong("company_id"),
      rs.getString("ip"),
      rs.getString("os"),
      rs.getString("browser"),
      rs.getString("user_agent")
    );
  }

}