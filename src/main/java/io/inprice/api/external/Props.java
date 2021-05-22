package io.inprice.api.external;

import io.inprice.common.config.SysProps;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.utils.NumberUtils;

public class Props {

  public static final int APP_PORT;
  public static final String APP_EMAIL_SENDER;
  public static final String APP_WEB_URL;
  public static final String APP_API_URL;
  public static final int APP_SALT_ROUNDS;
  public static final int APP_DAYS_FOR_FREE_USE;
  public static final int SERVICE_EXECUTION_THRESHOLD;

  public static final String API_KEYS_SENDGRID;
  public static final String API_KEYS_GELOCATION;

  public static final String INTERVAL_REMINDER_FOR_FREE_ACCOUNTS;
  public static final String INTERVAL_STOPPING_FREE_ACCOUNTS;
  public static final String INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS;
  public static final String INTERVAL_EXPIRING_PENDING_CHECKOUTS;
  public static final String INTERVAL_FLUSHING_ACCESS_LOG_QUEUE;

	public static final int TTL_FOR_COOKIES;
	
	static {
  	APP_PORT = NumberUtils.toInteger(System.getenv().getOrDefault("APP_PORT", SysProps.APP_ENV.equals(AppEnv.PROD) ? "8901" : "4567"));
  	APP_EMAIL_SENDER = System.getenv().getOrDefault("APP_EMAIL_SENDER", "account@inprice.io");
  	APP_WEB_URL = System.getenv().getOrDefault("APP_WEB_URL", "http://localhost:8080");
  	APP_API_URL = System.getenv().getOrDefault("APP_API_URL", "http://localhost:4567");
  	APP_SALT_ROUNDS = NumberUtils.toInteger(System.getenv().getOrDefault("APP_SALT_ROUNDS", SysProps.APP_ENV.equals(AppEnv.PROD) ? "6" : "1"));
  	APP_DAYS_FOR_FREE_USE = NumberUtils.toInteger(System.getenv().getOrDefault("APP_DAYS_FOR_FREE_USE", "14"));

  	API_KEYS_SENDGRID = System.getenv().get("API_KEYS_SENDGRID");
  	API_KEYS_GELOCATION = System.getenv().get("API_KEYS_GELOCATION");

  	INTERVAL_REMINDER_FOR_FREE_ACCOUNTS = System.getenv().getOrDefault("INTERVAL_REMINDER_FOR_FREE_ACCOUNTS", "1d");
    INTERVAL_STOPPING_FREE_ACCOUNTS = System.getenv().getOrDefault("INTERVAL_STOPPING_FREE_ACCOUNTS", "1h");
  	INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS = System.getenv().getOrDefault("INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS", "57m");
  	INTERVAL_EXPIRING_PENDING_CHECKOUTS =  System.getenv().getOrDefault("INTERVAL_EXPIRING_PENDING_CHECKOUTS", "5m");
  	INTERVAL_FLUSHING_ACCESS_LOG_QUEUE =  System.getenv().getOrDefault("INTERVAL_FLUSHING_ACCESS_LOG_QUEUE", "5m");

  	SERVICE_EXECUTION_THRESHOLD = NumberUtils.toInteger(System.getenv().getOrDefault("THRESHOLD_SERVICE_EXECUTION", "500")); //milliseconds

  	TTL_FOR_COOKIES = NumberUtils.toInteger(System.getenv().getOrDefault("TTL_FOR_COOKIES", "3600")); //one hour
	}

}
