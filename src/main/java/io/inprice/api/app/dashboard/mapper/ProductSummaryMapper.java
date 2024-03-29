package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.helpers.GlobalConsts;
import io.inprice.common.mappers.Helper;
import io.inprice.common.utils.StringHelper;

public class ProductSummaryMapper implements RowMapper<ProductSummary> {

  @Override
  public ProductSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    ProductSummary m = new ProductSummary();

    if (Helper.hasColumn(rs, "id")) m.setId(rs.getLong("id"));
    if (Helper.hasColumn(rs, "sku")) m.setSku(rs.getString("sku"));
    if (Helper.hasColumn(rs, "name")) m.setName(rs.getString("name"));
    if (Helper.hasColumn(rs, "actives")) m.setActives(rs.getInt("actives"));
    if (Helper.hasColumn(rs, "tryings")) m.setTryings(rs.getInt("tryings"));
    if (Helper.hasColumn(rs, "waitings")) m.setWaitings(rs.getInt("waitings"));
    if (Helper.hasColumn(rs, "problems")) m.setProblems(rs.getInt("problems"));

    if (Helper.hasColumn(rs, "price")) m.setPrice(rs.getBigDecimal("price"));
    if (Helper.hasColumn(rs, "min_price")) m.setMinPrice(rs.getBigDecimal("min_price"));
    if (Helper.hasColumn(rs, "avg_price")) m.setAvgPrice(rs.getBigDecimal("avg_price"));
    if (Helper.hasColumn(rs, "max_price")) m.setMaxPrice(rs.getBigDecimal("max_price"));

    if (Helper.hasColumn(rs, "min_seller")) m.setMinSeller(rs.getString("min_seller"));
    if (Helper.hasColumn(rs, "min_platform")) m.setMinPlatform(rs.getString("min_platform"));

    if (Helper.hasColumn(rs, "max_seller")) m.setMaxSeller(rs.getString("max_seller"));
    if (Helper.hasColumn(rs, "max_platform")) m.setMaxPlatform(rs.getString("max_platform"));
    
    int total = 0;
    if (m.getActives() != null) total += m.getActives();
    if (m.getTryings() != null) total += m.getTryings();
    if (m.getWaitings() != null) total += m.getWaitings();
    if (m.getProblems() != null) total += m.getProblems();
    m.setTotal(total);

    //sellers must be masked for demo account!
    if (rs.getLong("workspace_id") == GlobalConsts.DEMO_WS_ID) {
			if (StringUtils.isNotBlank(m.getMinSeller()) && GlobalConsts.YOU.equals(m.getMinSeller()) == false && GlobalConsts.NOT_AVAILABLE.equals(m.getMinSeller()) == false) {
				m.setMinSeller(StringHelper.maskString(m.getMinSeller()));
			}
			if (StringUtils.isNotBlank(m.getMaxSeller()) && GlobalConsts.YOU.equals(m.getMaxSeller()) == false && GlobalConsts.NOT_AVAILABLE.equals(m.getMaxSeller()) == false) {
				m.setMaxSeller(StringHelper.maskString(m.getMaxSeller()));
			}
    }

    return m;
  }

}