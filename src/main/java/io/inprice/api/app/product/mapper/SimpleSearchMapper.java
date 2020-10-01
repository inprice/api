package io.inprice.api.app.product.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class SimpleSearchMapper implements RowMapper<SimpleSearch> {

  @Override
  public SimpleSearch map(ResultSet rs, StatementContext ctx) throws SQLException {
    SimpleSearch m = new SimpleSearch();

    m.setId(rs.getLong("id"));
    m.setCode(rs.getString("code"));
    m.setName(rs.getString("name"));

    return m;
  }

}