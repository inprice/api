package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.utils.DateUtils;

public class Most10ProductMapper implements RowMapper<Most10Product> {

  @Override
  public Most10Product map(ResultSet rs, StatementContext ctx) throws SQLException {
    Most10Product m = new Most10Product();

    m.setId(rs.getLong("id"));
    m.setName(rs.getString("name"));
    m.setPrice(rs.getBigDecimal("price"));
    m.setRanking(rs.getInt("ranking"));
    m.setRankingWith(rs.getInt("ranking_with"));

    if (rs.getTimestamp("updated_at") != null) {
      m.setLastUpdate(DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
    } else {
      m.setLastUpdate(DateUtils.formatLongDate(rs.getTimestamp("created_at")));
    }

    return m;
  }

}