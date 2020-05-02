package io.inprice.scrapper.api.external;

import io.inprice.scrapper.api.utils.DateUtils;

public class Props {

   public static boolean isRunningForDev() {
      return ! "prod".equals(System.getenv().getOrDefault("APP_ENV", "prod").toLowerCase());
   }

   public static boolean isProdUniqueness() {
      return "true".equals(System.getenv().getOrDefault("APP_PROD_UNIQUENESS", "false").toLowerCase());
   }

   public static boolean isLinkUniqueness() {
      return "true".equals(System.getenv().getOrDefault("APP_LINK_UNIQUENESS", "false").toLowerCase());
   }

   public static boolean isShowingSQLQueries() {
      return "true".equals(System.getenv().getOrDefault("APP_SHOWING_SQL_QUERIES", "false").toLowerCase());
   }

   public static String getWebUrl() {
      return System.getenv().getOrDefault("APP_WEB_URL", "http://localhost:8080");
   }

   public static String getApiUrl() {
      return System.getenv().getOrDefault("APP_API_URL", "http://localhost:4567");
   }

   public static int getAPP_Port() {
      String def = isRunningForDev() ? "8901" : "4567";
      return new Integer(System.getenv().getOrDefault("APP_PORT", def));
   }

   public static int getAPP_WaitingTime() {
      return new Integer(System.getenv().getOrDefault("APP_WAITING_TIME", "5"));
   }

   public static String getDB_Driver() {
      String def = isRunningForDev() ? "h2" : "mysql";
      return System.getenv().getOrDefault("DB_DRIVER", def);
   }

   public static String getDB_Host() {
      String def = isRunningForDev() ? "mem" : "//localhost";
      return System.getenv().getOrDefault("DB_HOST", def);
   }

   public static int getDB_Port() {
      return new Integer(System.getenv().getOrDefault("DB_PORT", "3306"));
   }

   public static String getDB_Database() {
      String def = isRunningForDev() ? "test" : "inprice";
      return System.getenv().getOrDefault("DB_DATABASE", def);
   }

   public static String getDB_Username() {
      String def = isRunningForDev() ? "sa" : "root";
      return System.getenv().getOrDefault("DB_USERNAME", def);
   }

   public static String getDB_Password() {
      String def = isRunningForDev() ? "" : "1234";
      return System.getenv().getOrDefault("DB_PASSWORD", def);
   }

   public static String getDB_Additions() {
      String def = isRunningForDev()
            ? ";init=runscript from 'classpath:db/schema.sql'; runscript from 'classpath:db/data.sql'"
            : "";
      return System.getenv().getOrDefault("DB_ADDITIONS", def);
   }

   public static int getAS_SaltRounds() {
      String def = isRunningForDev() ? "4" : "12";
      return new Integer(System.getenv().getOrDefault("SECURITY_SALT_ROUNDS", def));
   }

   public static String getRedis_Host() {
      return System.getenv().getOrDefault("REDIS_HOST", "localhost");
   }

   public static int getRedis_Port() {
      return new Integer(System.getenv().getOrDefault("REDIS_PORT", "6379"));
   }

   public static String getRedis_Password() {
      return System.getenv().getOrDefault("REDIS_PASSWORD", null);
   }

   public static String getMQ_Host() {
      return System.getenv().getOrDefault("MQ_HOST", "localhost");
   }

   public static int getMQ_Port() {
      return new Integer(System.getenv().getOrDefault("MQ_PORT", "5672"));
   }

   public static String getMQ_Username() {
      return System.getenv().getOrDefault("MQ_USERNAME", "guest");
   }

   public static String getMQ_Password() {
      return System.getenv().getOrDefault("MQ_PASSWORD", "guest");
   }

   public static String getMQ_ChangeExchange() {
      return System.getenv().getOrDefault("MQ_EXCHANGE_CHANGES", "changes");
   }

   public static String getRouterKey_DeletedLinks() {
      return System.getenv().getOrDefault("MQ_ROUTING_DELETED_LINKS", "deleted-links");
   }

   public static Long getTTL_AccessTokens() {
      String def = isRunningForDev() ? "1m" : "15m";
      String ttl = System.getenv().getOrDefault("TTL_ACCESS_TOKENS", def);
      return DateUtils.parseTimePeriodAsMillis(ttl);
   }

   public static Long getTTL_RefreshTokens() {
      String def = isRunningForDev() ? "3m" : "1h";
      String ttl = System.getenv().getOrDefault("TTL_REFRESH_TOKENS", def);
      return DateUtils.parseTimePeriodAsMillis(ttl);
   }

   public static String getEmail_Sender() {
      return System.getenv().getOrDefault("OTHER_SENDER_EMAIL", "support@inprice.io");
   }

   public static String getPrefix_ForSearchingInEbay() {
      return System.getenv().getOrDefault("PREFIX_FOR_SEARCH_EBAY", "https://www.ebay.com/itm/");
   }

   public static String getPrefix_ForSearchingInAmazon() {
      return System.getenv().getOrDefault("PREFIX_FOR_SEARCH_AMAZON", "https://www.amazon.com/dp/");
   }

}