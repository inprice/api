package io.inprice.api.external;

import io.inprice.common.config.SysProps;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.utils.DateUtils;

public class Props {

  public static int APP_PORT() {
    String def = SysProps.APP_ENV().equals(AppEnv.PROD) ? "8901" : "4567";
    return Integer.parseInt(System.getenv().getOrDefault("APP_PORT", def));
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
    String def = SysProps.APP_ENV().equals(AppEnv.PROD) ? "6" : "1";
    return Integer.parseInt(System.getenv().getOrDefault("APP_SALT_ROUNDS", def));
  }

  public static int APP_DAYS_FOR_FREE_USE() {
    return Integer.parseInt(System.getenv().getOrDefault("APP_DAYS_FOR_FREE_USE", "15"));
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

  public static String API_KEYS_STRIPE() {
    return System.getenv().get("API_KEYS_STRIPE");
  }

  public static String API_KEYS_STRIPE_WEBHOOK() {
    return System.getenv().get("API_KEYS_STRIPE_WEBHOOK");
  }

  public static String TIME_PERIOD_OF_REMINDER_FOR_FREE_ACCOUNTS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_REMINDER_FOR_FREE_ACCOUNTS", "1d");
  }

  public static String TIME_PERIOD_OF_STOPPING_FREE_ACCOUNTS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_STOPPING_FREE_ACCOUNTS", "1h");
  }

  public static String TIME_PERIOD_OF_STOPPING_SUBSCRIBED_ACCOUNTS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_STOPPING_SUBSCRIBED_ACCOUNTS", "57m");
  }

  public static String TIME_PERIOD_OF_EXPIRING_PENDING_CHECKOUTS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_EXPIRING_PENDING_CHECKOUTS", "5m");
  }

}
