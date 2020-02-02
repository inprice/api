package io.inprice.scrapper.api.config;

import io.inprice.scrapper.common.utils.DateUtils;

public class Props {

	public static boolean isRunningForTests() {
		return "test".equals(System.getenv().getOrDefault(PropName.APP_RUNNING_AT.name(), "prod").toLowerCase());
	}

	public static boolean isProdUniqueness() {
		return "true".equals(System.getenv().getOrDefault(PropName.APP_PROD_UNIQUENESS.name(), "false").toLowerCase());
	}

	public static boolean isLinkUniqueness() {
		return "true".equals(System.getenv().getOrDefault(PropName.APP_LINK_UNIQUENESS.name(), "false").toLowerCase());
	}

	public static boolean isShowingSQLQueries() {
		return "true".equals(System.getenv().getOrDefault(PropName.APP_SHOWING_SQL_QUERIES.name(), "false").toLowerCase());
	}

	public static String getFrontendBaseUrl() {
		return System.getenv().getOrDefault(PropName.APP_SHOWING_SQL_QUERIES.name(), "http://localhost:8080");
	}

	public static int getAPP_Port() {
		String def = isRunningForTests() ? "8901": "4567";
		return new Integer(System.getenv().getOrDefault(PropName.APP_PORT.name(), def));
	}

	public static int getAPP_WaitingTime() {
		return new Integer(System.getenv().getOrDefault(PropName.APP_WAITING_TIME.name(), "5"));
	}
	public static String getDB_Driver() {
		String def = isRunningForTests() ? "h2": "mysql";
		return System.getenv().getOrDefault(PropName.DB_DRIVER.name(), def);
	}

	public static String getDB_Host() {
		String def = isRunningForTests() ? "mem": "//localhost";
		return System.getenv().getOrDefault(PropName.DB_HOST.name(), def);
	}

	public static int getDB_Port() {
		return new Integer(System.getenv().getOrDefault(PropName.DB_PORT.name(), "3306"));
	}
	
	public static String getDB_Database() {
		String def = isRunningForTests() ? "test": "inprice";
		return System.getenv().getOrDefault(PropName.DB_DATABASE.name(), def);
	}
	
	public static String getDB_Username() {
		String def = isRunningForTests() ? "sa": "root";
		return System.getenv().getOrDefault(PropName.DB_USERNAME.name(), def);
	}
	
	public static String getDB_Password() {
		String def = isRunningForTests() ? "": "1234";
		return System.getenv().getOrDefault(PropName.DB_PASSWORD.name(), def);
	}

	public static String getDB_Additions() {
		String def = isRunningForTests() ? ";init=runscript from 'classpath:db/schema.sql'; runscript from 'classpath:db/data.sql'" : "";
		return System.getenv().getOrDefault(PropName.DB_ADDITIONS.name(), def);
	}

	public static int getAS_SaltRounds() {
		String def = isRunningForTests() ? "4": "12";
		return new Integer(System.getenv().getOrDefault(PropName.SECURITY_SALT_ROUNDS.name(), def));
	}

	public static String getRedis_Host() {
		return System.getenv().getOrDefault(PropName.REDIS_HOST.name(), "localhost");
	}

	public static int getRedis_Port() {
		return new Integer(System.getenv().getOrDefault(PropName.REDIS_PORT.name(), "6379"));
	}

	public static String getRedis_Password() {
		return System.getenv().getOrDefault(PropName.REDIS_PASSWORD.name(), null);
	}


	public static String getMQ_Host() {
		return System.getenv().getOrDefault(PropName.MQ_HOST.name(), "localhost");
	}

	public static int getMQ_Port() {
		return new Integer(System.getenv().getOrDefault(PropName.MQ_PORT.name(), "5672"));
	}

	public static String getMQ_Username() {
		return System.getenv().getOrDefault(PropName.MQ_USERNAME.name(), "guest");
	}

	public static String getMQ_Password() {
		return System.getenv().getOrDefault(PropName.MQ_PASSWORD.name(), "guest");
	}

	public static String getMQ_ChangeExchange() {
		return System.getenv().getOrDefault(PropName.MQ_EXCHANGE_CHANGES.name(), "changes");
	}

	public static String getRoutingKey_DeletedLinks() {
		return System.getenv().getOrDefault(PropName.MQ_ROUTING_DELETED_LINKS.name(), "deleted-links");
	}

	public static Long getTTL_AccessTokens() {
		String def = isRunningForTests() ? "3m": "15m";
		String ttl = System.getenv().getOrDefault(PropName.TTL_ACCESS_TOKENS.name(), def);
		return DateUtils.parseTimePeriodAsMillis(ttl);
	}

	public static Long getTTL_RefreshTokens() {
		String def = isRunningForTests() ? "5h": "2h";
		String ttl = System.getenv().getOrDefault(PropName.TTL_REFRESH_TOKENS.name(), def);
		return DateUtils.parseTimePeriodAsMillis(ttl);
	}

	public static String getEmail_Sender() {
		return System.getenv().getOrDefault(PropName.OTHER_SENDER_EMAIL.name(), "support@inprice.io");
	}

	public static String getEmail_APIKey() {
		return System.getenv().getOrDefault(PropName.API_KEY_SENDGRID.name(), "test");
	}

	public static String getPrefix_ForSearchingInEbay() {
		return System.getenv().getOrDefault(PropName.PREFIX_FOR_SEARCH_EBAY.name(), "https://www.ebay.com/itm/");
	}

	public static String getPrefix_ForSearchingInAmazon() {
		return System.getenv().getOrDefault(PropName.PREFIX_FOR_SEARCH_AMAZON.name(), "https://www.amazon.com/dp/");
	}

}
