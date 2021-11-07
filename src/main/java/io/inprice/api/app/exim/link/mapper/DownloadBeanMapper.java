package io.inprice.api.app.exim.link.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class DownloadBeanMapper implements RowMapper<DownloadBean> {

  @Override
  public DownloadBean map(ResultSet rs, StatementContext ctx) throws SQLException {
    DownloadBean m = new DownloadBean();

    if (Helper.hasColumn(rs, "sku")) m.setSku(rs.getString("sku"));
    if (Helper.hasColumn(rs, "url")) m.setUrl(rs.getString("url"));

    return m;
  }

}