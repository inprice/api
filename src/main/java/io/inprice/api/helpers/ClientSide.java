package io.inprice.api.helpers;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Consts;
import io.inprice.api.external.Props;
import io.inprice.api.utils.CurrencyFormats;
import io.inprice.common.config.SysProps;
import io.inprice.common.meta.AppEnv;

/**
 * Client side operations
 */
public class ClientSide {

  private static final Logger log = LoggerFactory.getLogger(ClientSide.class);


  private static final String[] 
      HEADERS_TO_TRY = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
      };

  public static String getIp(HttpServletRequest req) {
    for (String header: HEADERS_TO_TRY) {
      String ip = req.getHeader(header);
      if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
        return ip.contains(",") ? ip.split(",")[0] : ip;
      }
    }
    return req.getRemoteAddr();
  }

  public static Map<String, String> getGeoInfo(HttpServletRequest req) {
    Map<String, String> result = new HashMap<>(4);
    result.put(Consts.IP, "127.0.0.1");
    result.put(Consts.TIMEZONE, "Europe/Dublin");
    result.put(Consts.CURRENCY_CODE, "USD");
    result.put(Consts.CURRENCY_FORMAT, "$#,###.00;$-#,###.00");
    
    if (! SysProps.APP_ENV.equals(AppEnv.TEST)) {
      String ip = getIp(req);
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
  
        if (ip.equals("127.0.0.1")) {
          ip = HttpHelper.GET(httpClient, "http://checkip.amazonaws.com");
        }
  
        if (ip != null && !ip.equals("127.0.0.1")) {
          result.put(Consts.IP, ip);
          String res = 
            HttpHelper.GET(
              httpClient, String.format("https://api.ipgeolocation.io/ipgeo?apiKey=%s&ip=%s",
                Props.API_KEYS_GELOCATION, URLEncoder.encode(ip, "UTF-8")
              )
            );
          if (res != null) {
            JSONObject root = new JSONObject(res);
            JSONObject currency = root.getJSONObject("currency"); 
            JSONObject timezone = root.getJSONObject("time_zone");
            result.put(Consts.CURRENCY_CODE, currency.getString("code"));
            result.put(Consts.CURRENCY_FORMAT, CurrencyFormats.get(currency.getString("code")));
            result.put(Consts.TIMEZONE, timezone.getString("name"));
          }
        }
      } catch (IOException e) {
        log.error("Failed to get geo info", e);
      }
    }
    return result;
  }

}