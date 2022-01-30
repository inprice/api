package io.inprice.api.app.exim.link.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import io.inprice.common.helpers.GlobalConsts;
import io.inprice.common.mappers.Helper;

public class DownloadBeanMapper implements RowMapper<DownloadBean> {

  @Override
  public DownloadBean map(ResultSet rs, StatementContext ctx) throws SQLException {
    DownloadBean m = new DownloadBean();

    if (Helper.hasColumn(rs, "sku")) m.setSku(rs.getString("sku"));
    if (Helper.hasColumn(rs, "url")) m.setUrl(rs.getString("url"));

    //url (not sku since it belongs to product) must be masked for demo account!
		if (rs.getLong("workspace_id") == GlobalConsts.DEMO_WS_ID) {
			m.setUrl(m.getUrl().substring(0, m.getUrl().length()-12) + "-masked-url");
		}

    return m;
  }

}