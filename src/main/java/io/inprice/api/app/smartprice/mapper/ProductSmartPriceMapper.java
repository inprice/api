package io.inprice.api.app.smartprice.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class ProductSmartPriceMapper implements RowMapper<ProductSmartPrice> {

  @Override
  public ProductSmartPrice map(ResultSet rs, StatementContext ctx) throws SQLException {
  	ProductSmartPrice m = new ProductSmartPrice();

  	//product
    if (Helper.hasColumn(rs, "product_id")) m.setProductId(rs.getLong("product_id"));
    if (Helper.hasColumn(rs, "actives")) m.setActives(rs.getInt("actives"));
    if (Helper.hasColumn(rs, "price")) m.setPrice(rs.getBigDecimal("price"));
    if (Helper.hasColumn(rs, "base_price")) m.setBasePrice(rs.getBigDecimal("base_price"));
    if (Helper.hasColumn(rs, "min_price")) m.setMinPrice(rs.getBigDecimal("min_price"));
    if (Helper.hasColumn(rs, "avg_price")) m.setAvgPrice(rs.getBigDecimal("avg_price"));
    if (Helper.hasColumn(rs, "max_price")) m.setMaxPrice(rs.getBigDecimal("max_price"));

    //smartprice
    if (Helper.hasColumn(rs, "formula")) m.setFormula(rs.getString("formula"));
    if (Helper.hasColumn(rs, "lower_limit_formula")) m.setLowerLimitFormula(rs.getString("lower_limit_formula"));
    if (Helper.hasColumn(rs, "upper_limit_formula")) m.setUpperLimitFormula(rs.getString("upper_limit_formula"));

    return m;
  }

}