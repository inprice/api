package io.inprice.api.app.dashboard.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.api.helpers.HttpHelper;
import io.inprice.common.mappers.Helper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.utils.DateUtils;

public class MRU25LinkMapper implements RowMapper<MRU25Link> {

  @Override
  public MRU25Link map(ResultSet rs, StatementContext ctx) throws SQLException {
    MRU25Link m = new MRU25Link();

    if (Helper.hasColumn(rs, "id")) m.setId(rs.getLong("id"));
    if (Helper.hasColumn(rs, "product_id")) m.setProductId(rs.getLong("product_id"));
    if (Helper.hasColumn(rs, "product_name")) m.setProductName(rs.getString("product_name"));
    if (Helper.hasColumn(rs, "seller")) m.setSeller(rs.getString("seller"));
    if (Helper.hasColumn(rs, "name")) m.setName(rs.getString("name"));
    if (Helper.hasColumn(rs, "url")) m.setUrl(rs.getString("url"));
    if (Helper.hasColumn(rs, "price")) m.setPrice(rs.getBigDecimal("price"));
    if (Helper.hasColumn(rs, "position")) m.setPosition(rs.getString("position"));
    if (Helper.hasColumn(rs, "parse_code")) m.setStatusDesc(rs.getString("parse_code"));
    if (Helper.hasColumn(rs, "alarm_id")) m.setAlarmId(Helper.nullLongHandler(rs, "alarm_id"));

    if (Helper.hasColumn(rs, "updated_at") && rs.getTimestamp("updated_at") != null) {
      m.setUpdatedAt(DateUtils.formatLongDate(rs.getTimestamp("updated_at")));
    } else if (Helper.hasColumn(rs, "created_at")) {
      m.setUpdatedAt(DateUtils.formatLongDate(rs.getTimestamp("created_at")));
    }

    if (Helper.hasColumn(rs, "status")) {
    	String val = rs.getString("status");
    	if (val != null) {
    		LinkStatus linkStatus = LinkStatus.valueOf(val);
    		m.setStatus(linkStatus.getGrup().name());
    	}
    }
    
    String platform = null;
    if (Helper.hasColumn(rs, "platform")) platform = rs.getString("platform");
  	if (StringUtils.isBlank(platform) && Helper.hasColumn(rs, "url") ) {
      platform = HttpHelper.extractHostname(rs.getString("url"));
    }
    m.setPlatform(platform);

    return m;
  }

}