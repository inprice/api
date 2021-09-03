package io.inprice.api.app.system.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.api.app.system.model.Search;
import io.inprice.common.mappers.Helper;

public class SearchMapper implements RowMapper<Search> {

  @Override
  public Search map(ResultSet rs, StatementContext ctx) throws SQLException {
  	Search m = new Search();

    if (Helper.hasColumn(rs, "id")) m.setId(rs.getLong("id"));
    if (Helper.hasColumn(rs, "type")) m.setType(rs.getString("type"));
    if (Helper.hasColumn(rs, "name")) m.setName(rs.getString("name"));
    if (Helper.hasColumn(rs, "description")) m.setDescription(rs.getString("description"));

    return m;
  }

}