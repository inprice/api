package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;
import io.inprice.common.utils.DateUtils;

public class ProductSummaryMapper implements RowMapper<ProductSummary> {

  @Override
  public ProductSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    ProductSummary m = new ProductSummary();

    if (Helper.hasColumn(rs, "id")) m.setId(rs.getLong("id"));
    if (Helper.hasColumn(rs, "name")) m.setName(rs.getString("name"));
    if (Helper.hasColumn(rs, "description")) m.setDescription(rs.getString("description"));
    if (Helper.hasColumn(rs, "actives")) m.setActives(rs.getInt("actives"));
    if (Helper.hasColumn(rs, "tryings")) m.setTryings(rs.getInt("tryings"));
    if (Helper.hasColumn(rs, "waitings")) m.setWaitings(rs.getInt("waitings"));
    if (Helper.hasColumn(rs, "problems")) m.setProblems(rs.getInt("problems"));
    if (Helper.hasColumn(rs, "price")) m.setPrice(rs.getBigDecimal("price"));

    if (Helper.hasColumn(rs, "actives_sum")) m.setActives(rs.getInt("actives_sum"));
    if (Helper.hasColumn(rs, "tryings_sum")) m.setTryings(rs.getInt("tryings_sum"));
    if (Helper.hasColumn(rs, "waitings_sum")) m.setWaitings(rs.getInt("waitings_sum"));
    if (Helper.hasColumn(rs, "problems_sum")) m.setProblems(rs.getInt("problems_sum"));
    
    if (Helper.hasColumn(rs, "updated_at") && rs.getTimestamp("updated_at") != null) {
      m.setUpdatedAt(DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
    } else if (Helper.hasColumn(rs, "created_at")) {
      m.setUpdatedAt(DateUtils.formatLongDate(rs.getTimestamp("created_at")));
    }
    
    int total = 0;
    if (m.getActives() != null) total += m.getActives();
    if (m.getTryings() != null) total += m.getTryings();
    if (m.getWaitings() != null) total += m.getWaitings();
    if (m.getProblems() != null) total += m.getProblems();
    m.setTotal(total);

    return m;
  }

}