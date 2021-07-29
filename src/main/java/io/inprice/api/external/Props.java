package io.inprice.api.external;

import io.inprice.common.utils.NumberUtils;

public class Props {

  public static final int APP_PORT;
  public static final String APP_WEB_URL;
  public static final String APP_API_URL;
  public static final int APP_SALT_ROUNDS;
  public static final int APP_DAYS_FOR_FREE_USE;
  public static final int SERVICE_EXECUTION_THRESHOLD;

  public static final String API_KEYS_GELOCATION;
	public static final int TTL_FOR_COOKIES;

	static {
  	APP_PORT = NumberUtils.toInteger(System.getenv().getOrDefault("APP_PORT", "4567"));
  	APP_WEB_URL = System.getenv().getOrDefault("APP_WEB_URL", "http://localhost:8080");
  	APP_API_URL = System.getenv().getOrDefault("APP_API_URL", "http://localhost:" + APP_PORT);
  	APP_SALT_ROUNDS = NumberUtils.toInteger(System.getenv().getOrDefault("APP_SALT_ROUNDS", "6"));
  	APP_DAYS_FOR_FREE_USE = NumberUtils.toInteger(System.getenv().getOrDefault("APP_DAYS_FOR_FREE_USE", "14"));

  	API_KEYS_GELOCATION = System.getenv().getOrDefault("API_KEYS_GELOCATION", "9eac35ec732e43a3a89863fce6cb406a");

  	SERVICE_EXECUTION_THRESHOLD = NumberUtils.toInteger(System.getenv().getOrDefault("THRESHOLD_SERVICE_EXECUTION", "500")); //milliseconds
  	TTL_FOR_COOKIES = NumberUtils.toInteger(System.getenv().getOrDefault("TTL_FOR_COOKIES", "3600")); //one hour
	}

}
