package io.inprice.scrapper.api.external;

import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.meta.AppEnv;
import io.inprice.scrapper.common.utils.DateUtils;

public class Props {

  public static int APP_PORT() {
    String def = SysProps.APP_ENV().equals(AppEnv.PROD) ? "8901" : "4567";
    return new Integer(System.getenv().getOrDefault("APP_PORT", def));
  }

  public static String APP_EMAIL_SENDER() {
    return System.getenv().getOrDefault("APP_EMAIL_SENDER", "account@inprice.io");
  }

  public static String APP_WEB_URL() {
    return System.getenv().getOrDefault("APP_WEB_URL", "http://localhost:8080");
  }

  public static String APP_API_URL() {
    return System.getenv().getOrDefault("APP_API_URL", "http://localhost:4567");
  }

  public static int APP_SALT_ROUNDS() {
    String def = SysProps.APP_ENV().equals(AppEnv.PROD) ? "12" : "4";
    return new Integer(System.getenv().getOrDefault("APP_SALT_ROUNDS", def));
  }

  public static Long TTL_ACCESS_TOKENS() {
    String def = SysProps.APP_ENV().equals(AppEnv.PROD) ? "15m" : "1m";
    String ttl = System.getenv().getOrDefault("TTL_ACCESS_TOKENS", def);
    return DateUtils.parseTimePeriodAsMillis(ttl);
  }

  public static Long TTL_REFRESH_TOKENS() {
    String def = SysProps.APP_ENV().equals(AppEnv.PROD) ? "1h" : "3m";
    String ttl = System.getenv().getOrDefault("TTL_REFRESH_TOKENS", def);
    return DateUtils.parseTimePeriodAsMillis(ttl);
  }

  public static String PREFIX_FOR_SEARCH_EBAY() {
    return System.getenv().getOrDefault("PREFIX_FOR_SEARCH_EBAY", "https://www.ebay.com/itm/");
  }

  public static String PREFIX_FOR_SEARCH_AMAZON() {
    return System.getenv().getOrDefault("PREFIX_FOR_SEARCH_AMAZON", "https://www.amazon.com/dp/");
  }

  public static String API_KEYS_SENDGRID() {
    return System.getenv().get("API_KEYS_SENDGRID");
  }

  public static String API_KEYS_GELOCATION() {
    return System.getenv().get("API_KEYS_GELOCATION");
  }

}
