package io.inprice.api.app.alarm.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class AlarmEntityMapper implements RowMapper<AlarmEntity> {

  @Override
  public AlarmEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
    AlarmEntity m = new AlarmEntity();

    m.setId(rs.getLong("id"));
    if (Helper.hasColumn(rs, "sku")) m.setSku(rs.getString("sku"));
    if (Helper.hasColumn(rs, "name")) m.setName(rs.getString("name"));
    if (Helper.hasColumn(rs, "position")) m.setPosition(rs.getString("position"));
    if (Helper.hasColumn(rs, "price")) m.setPrice(rs.getBigDecimal("price"));
    if (Helper.hasColumn(rs, "alarmed_at")) m.setAlarmedAt(rs.getTimestamp("alarmed_at"));
    
    return m;
  }

}