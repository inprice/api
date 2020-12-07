package io.inprice.api.app.company.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class CompanyIdUserEmailMapper implements RowMapper<CompanyIdUserEmail> {

  @Override
  public CompanyIdUserEmail map(ResultSet rs, StatementContext ctx) throws SQLException {
    CompanyIdUserEmail m = new CompanyIdUserEmail();

    if (Helper.hasColumn(rs, "id")) m.setCompanyId(Helper.nullLongHandler(rs, "id"));
    if (Helper.hasColumn(rs, "email")) m.setEmail(rs.getString("email"));

    return m;
  }

}