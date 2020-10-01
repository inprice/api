package io.inprice.api.app.membership.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;
import io.inprice.common.utils.DateUtils;

public class ActiveMembershipMapper implements RowMapper<ActiveMembership> {

  @Override
  public ActiveMembership map(ResultSet rs, StatementContext ctx) throws SQLException {
    ActiveMembership m = new ActiveMembership();

    m.setId(rs.getLong("id"));
    m.setName(rs.getString("name"));
    m.setRole(rs.getString("role"));
    if (Helper.hasColumn(rs, "updated_at")) m.setDate(DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
    if (Helper.hasColumn(rs, "created_at")) m.setDate(DateUtils.formatLongDate(rs.getTimestamp("created_at")));

    return m;
  }

}