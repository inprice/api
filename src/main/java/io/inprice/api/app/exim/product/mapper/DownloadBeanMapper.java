package io.inprice.api.app.exim.product.mapper;

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
    if (Helper.hasColumn(rs, "name")) m.setName(rs.getString("name"));
    if (Helper.hasColumn(rs, "price")) m.setPrice(rs.getDouble("price"));
    if (Helper.hasColumn(rs, "brand_name")) m.setBrandName(rs.getString("brand_name"));
    if (Helper.hasColumn(rs, "category_name")) m.setCategoryName(rs.getString("category_name"));

    return m;
  }

}