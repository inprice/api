package io.inprice.api.app.company.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class CompanyInfoMapper implements RowMapper<CompanyInfo> {

  @Override
  public CompanyInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
    CompanyInfo m = new CompanyInfo();

    if (Helper.hasColumn(rs, "id")) m.setId(Helper.nullLongHandler(rs, "id"));
    if (Helper.hasColumn(rs, "name")) m.setEmail(rs.getString("name"));
    if (Helper.hasColumn(rs, "email")) m.setEmail(rs.getString("email"));
    if (Helper.hasColumn(rs, "cust_id")) m.setCustId(rs.getString("cust_id"));

    return m;
  }

}