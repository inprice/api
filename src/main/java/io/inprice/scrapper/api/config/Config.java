package io.inprice.scrapper.api.config;

public class Config {

	// DB
	public static final String DB_HOST;
	public static final int DB_PORT;
	public static final String DB_DATABASE;
	public static final String DB_USERNAME;
	public static final String DB_PASSWORD;

	static {
		DB_HOST = getOrDefault("DB_HOST", "localhost");
		DB_PORT = getOrDefault("DB_PORT", 3306);
		DB_DATABASE = getOrDefault("DB_DATABASE", "inprice");
		DB_USERNAME = getOrDefault("DB_USERNAME", "root");
		DB_PASSWORD = getOrDefault("DB_PASSWORD", "1234");
	}

	private static String getOrDefault(String key, String defauld) {
		String val = System.getenv(key);
		if (val != null && val.trim().length() > 0) return val;
		return defauld;
	}

	private static int getOrDefault(String key, int defauld) {
		String val = System.getenv(key);
		if (val != null && val.trim().length() > 0) return Integer.parseInt(val.trim());
		return defauld;
	}

	private static long getOrDefault(String key, long defauld) {
		String val = System.getenv(key);
		if (val != null && val.trim().length() > 0) return Long.parseLong(val.trim());
		return defauld;
	}

}
