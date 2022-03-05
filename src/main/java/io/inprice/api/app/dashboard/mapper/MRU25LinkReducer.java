package io.inprice.api.app.dashboard.mapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import io.inprice.api.helpers.HttpHelper;
import io.inprice.common.helpers.GlobalConsts;
import io.inprice.common.mappers.Helper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.utils.DateUtils;
import io.inprice.common.utils.StringHelper;

public class MRU25LinkReducer implements LinkedHashMapRowReducer<Long, MRU25Link> {

  @Override
  public void accumulate(Map<Long, MRU25Link> map, RowView rw) {
      MRU25Link m = map.get(Helper.getColumnVal(rw, "id", Long.class));
      
      if (m == null) {
      	m = new MRU25Link();
      	m.setId(Helper.getColumnVal(rw, "id", Long.class));

        m.setProductId(Helper.getColumnVal(rw, "product_id", Long.class));
        m.setProductName(Helper.getColumnVal(rw, "product_name", String.class));
        m.setName(Helper.getColumnVal(rw, "name", String.class));
        m.setUrl(Helper.getColumnVal(rw, "url", String.class));
        m.setSeller(Helper.getColumnVal(rw, "seller", String.class));
        m.setPrice(Helper.getColumnVal(rw, "price", BigDecimal.class));
        m.setPosition(Helper.getColumnVal(rw, "position", String.class));
        m.setStatusDesc(Helper.getColumnVal(rw, "parse_code", String.class));
      	m.setAlarmId(Helper.getColumnVal(rw, "alarm_id", Long.class));
        m.setAlarmName(Helper.getColumnVal(rw, "al_name", String.class));

        m.setUpdatedAt(DateUtils.formatLongDate(Helper.getColumnVal(rw, "updated_at", Timestamp.class, Helper.getColumnVal(rw, "created_at", Timestamp.class))));

      	String val = Helper.getColumnVal(rw, "status", String.class);
      	if (val != null) {
      		LinkStatus linkStatus = LinkStatus.valueOf(val);
      		m.setStatus(linkStatus.getGrup().name());
      	}
        
        String domain = Helper.getColumnVal(rw, "domain", String.class);
      	if (StringUtils.isBlank(domain) && StringUtils.isNotBlank(m.getUrl())) {
          domain = HttpHelper.extractHostname(m.getUrl());
        }
        m.setPlatform(domain);

        //seller and url must be masked for demo account!
        Long workspaceId = Helper.getColumnVal(rw, "workspace_id", Long.class, 0l);
    		if (workspaceId == GlobalConsts.DEMO_WS_ID) {
    			String maskedSeller = (StringUtils.isNotBlank(m.getSeller()) && GlobalConsts.NOT_AVAILABLE.equals(m.getSeller()) == false ? StringHelper.maskString(m.getSeller()) : null);
    			if (maskedSeller != null) {
    				m.setUrl(m.getUrl().replaceAll(m.getSeller(), maskedSeller));
    				m.setSeller(maskedSeller);
    			}
    			m.setUrl(m.getUrl().substring(0, m.getUrl().length()-12) + "-masked-url");
    		}
      }
      
    	BigDecimal lpPrice = Helper.getColumnVal(rw, "lp_price", BigDecimal.class, BigDecimal.ZERO);
      m.getPrices().add(lpPrice);
      //duplication for the first price is needed because sparkline component at fe side doesn't render properly without this!!!
      if (m.getPrices().size() == 1) m.getPrices().add(lpPrice);
      if (m.getPrices().size() > 25) m.getPrices().remove(0);

      map.put(m.getId(), m);
  }

}