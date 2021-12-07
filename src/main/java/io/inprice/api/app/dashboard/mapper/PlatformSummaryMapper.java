package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.mappers.Helper;

public class PlatformSummaryMapper implements RowMapper<PlatformSummary> {

  @Override
  public PlatformSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    PlatformSummary m = new PlatformSummary();

    if (Helper.hasColumn(rs, "domain")) m.setDomain(rs.getString("domain"));
    if (Helper.hasColumn(rs, "actives")) m.setActives(rs.getInt("actives"));
    if (Helper.hasColumn(rs, "tryings")) m.setTryings(rs.getInt("tryings"));
    if (Helper.hasColumn(rs, "waitings")) m.setWaitings(rs.getInt("waitings"));
    if (Helper.hasColumn(rs, "problems")) m.setProblems(rs.getInt("problems"));

    int total = 0;
    if (m.getActives() != null) total += m.getActives();
    if (m.getTryings() != null) total += m.getTryings();
    if (m.getWaitings() != null) total += m.getWaitings();
    if (m.getProblems() != null) total += m.getProblems();
    m.setTotal(total);

    return m;
  }

}