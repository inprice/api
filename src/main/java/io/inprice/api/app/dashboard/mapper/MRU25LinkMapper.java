package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.api.helpers.HttpHelper;
import io.inprice.common.utils.DateUtils;

public class MRU25LinkMapper implements RowMapper<MRU25Link> {

  @Override
  public MRU25Link map(ResultSet rs, StatementContext ctx) throws SQLException {
    MRU25Link m = new MRU25Link();

    m.setGroupName(rs.getString("group_name"));
    m.setSeller(rs.getString("seller"));
    m.setUrl(rs.getString("url"));
    m.setPrice(rs.getBigDecimal("price"));
    m.setStatus(rs.getString("status"));
    m.setLevel(rs.getString("level"));

    if (rs.getTimestamp("last_update") != null) {
      m.setLastUpdate(DateUtils.formatLongDate(rs.getTimestamp("last_update")));
    } else {
      m.setLastUpdate(DateUtils.formatLongDate(rs.getTimestamp("created_at")));
    }

    String platform = rs.getString("platform");
    if (StringUtils.isBlank(platform)) {
      platform = HttpHelper.extractHostname(rs.getString("url"));
    }
    m.setPlatform(platform);

    return m;
  }

}