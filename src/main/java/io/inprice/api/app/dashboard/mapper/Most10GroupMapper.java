package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.utils.DateUtils;

public class Most10GroupMapper implements RowMapper<Most10Group> {

  @Override
  public Most10Group map(ResultSet rs, StatementContext ctx) throws SQLException {
    Most10Group m = new Most10Group();

    m.setId(rs.getLong("id"));
    m.setName(rs.getString("name"));
    m.setActives(rs.getInt("actives"));
    m.setTryings(rs.getInt("tryings"));
    m.setWaitings(rs.getInt("waitings"));
    m.setProblems(rs.getInt("problems"));
    m.setPrice(rs.getBigDecimal("price"));

    if (rs.getTimestamp("updated_at") != null) {
      m.setLastUpdate(DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
    } else {
      m.setLastUpdate(DateUtils.formatLongDate(rs.getTimestamp("created_at")));
    }

    return m;
  }

}