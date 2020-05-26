package io.inprice.scrapper.api.external;

import io.inprice.scrapper.common.utils.DateUtils;

public class Props {

  public static boolean IS_RUN_FOR_DEV() {
    return !"prod".equals(System.getenv().getOrDefault("APP_ENV", "prod").toLowerCase());
  }

  public static int APP_PORT() {
    String def = IS_RUN_FOR_DEV() ? "8901" : "4567";
    return new Integer(System.getenv().getOrDefault("APP_PORT", def));
  }

  public static String APP_EMAIL_SENDER() {
    return System.getenv().getOrDefault("APP_EMAIL_SENDER", "support@inprice.io");
  }

  public static boolean APP_SHOW_QUERIES() {
    return "true".equals(System.getenv().getOrDefault("APP_SHOW_QUERIES", "false").toLowerCase());
  }

  public static String APP_WEB_URL() {
    return System.getenv().getOrDefault("APP_WEB_URL", "http://localhost:8080");
  }

  public static String APP_API_URL() {
    return System.getenv().getOrDefault("APP_API_URL", "http://localhost:4567");
  }

  public static int APP_SALT_ROUNDS() {
    String def = IS_RUN_FOR_DEV() ? "4" : "12";
    return new Integer(System.getenv().getOrDefault("APP_SALT_ROUNDS", def));
  }

  public static String DB_DRIVER() {
    String def = IS_RUN_FOR_DEV() ? "h2" : "mysql";
    return System.getenv().getOrDefault("DB_DRIVER", def);
  }

  public static String DB_HOST() {
    String def = IS_RUN_FOR_DEV() ? "mem" : "//localhost";
    return System.getenv().getOrDefault("DB_HOST", def);
  }

  public static int DB_PORT() {
    return new Integer(System.getenv().getOrDefault("DB_PORT", "3306"));
  }

  public static String DB_DATABASE() {
    String def = IS_RUN_FOR_DEV() ? "test" : "inprice";
    return System.getenv().getOrDefault("DB_DATABASE", def);
  }

  public static String DB_USERNAME() {
    String def = IS_RUN_FOR_DEV() ? "sa" : "root";
    return System.getenv().getOrDefault("DB_USERNAME", def);
  }

  public static String DB_PASSWORD() {
    String def = IS_RUN_FOR_DEV() ? "" : "1234";
    return System.getenv().getOrDefault("DB_PASSWORD", def);
  }

  public static String DB_ADDITIONS() {
    String def = IS_RUN_FOR_DEV()
        ? ";init=runscript from 'classpath:db/schema.sql'; runscript from 'classpath:db/data.sql'"
        : "";
    return System.getenv().getOrDefault("DB_ADDITIONS", def);
  }

  public static String REDIS_HOST() {
    return System.getenv().getOrDefault("REDIS_HOST", "localhost");
  }

  public static int REDIS_PORT() {
    return new Integer(System.getenv().getOrDefault("REDIS_PORT", "6379"));
  }

  public static String REDIS_PASSWORD() {
    return System.getenv().getOrDefault("REDIS_PASSWORD", null);
  }

  public static String MQ_HOST() {
    return System.getenv().getOrDefault("MQ_HOST", "localhost");
  }

  public static int MQ_PORT() {
    return new Integer(System.getenv().getOrDefault("MQ_PORT", "5672"));
  }

  public static String MQ_USERNAME() {
    return System.getenv().getOrDefault("MQ_USERNAME", "guest");
  }

  public static String MQ_PASSWORD() {
    return System.getenv().getOrDefault("MQ_PASSWORD", "guest");
  }

  public static String MQ_EXCHANGE_CHANGES() {
    return System.getenv().getOrDefault("MQ_EXCHANGE_CHANGES", "changes");
  }

  public static String MQ_ROUTING_DELETED_LINKS() {
    return System.getenv().getOrDefault("MQ_ROUTING_DELETED_LINKS", "deleted-links");
  }

  public static Long TTL_ACCESS_TOKENS() {
    String def = IS_RUN_FOR_DEV() ? "1m" : "15m";
    String ttl = System.getenv().getOrDefault("TTL_ACCESS_TOKENS", def);
    return DateUtils.parseTimePeriodAsMillis(ttl);
  }

  public static Long TTL_REFRESH_TOKENS() {
    String def = IS_RUN_FOR_DEV() ? "3m" : "1h";
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
