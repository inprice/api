package io.inprice.api.app.account.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class AccountInfoMapper implements RowMapper<AccountInfo> {

  @Override
  public AccountInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
    AccountInfo m = new AccountInfo();

    if (Helper.hasColumn(rs, "id")) m.setId(Helper.nullLongHandler(rs, "id"));
    if (Helper.hasColumn(rs, "name")) m.setEmail(rs.getString("name"));
    if (Helper.hasColumn(rs, "email")) m.setEmail(rs.getString("email"));
    if (Helper.hasColumn(rs, "cust_id")) m.setCustId(rs.getString("cust_id"));

    return m;
  }

}