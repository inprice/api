package io.inprice.scrapper.api.config;

import io.inprice.scrapper.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class Properties {

	private static final Logger log = LoggerFactory.getLogger(Properties.class);

	private final java.util.Properties prop;

	Properties() {
		prop = new java.util.Properties();

		try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			if (input == null) {
				log.error("Unable to find config.props in class path!");
				return;
			}
			prop.load(input);
		} catch (IOException e) {
			log.error("Failed to load config.properties", e);
		}
	}

	public boolean isRunningForTests() {
		String runningAt = prop.getProperty("app.running-at", "test");
		return runningAt.equals("test");
	}

	public boolean isProdUniqueness() {
		String uniqueness = prop.getProperty("app.prod-uniqueness", "false");
		return uniqueness.equals("true");
	}

	public boolean isLinkUniqueness() {
		String uniqueness = prop.getProperty("app.link-uniqueness", "false");
		return uniqueness.equals("true");
	}

	public boolean isShowingSQLQueries() {
		String showQueries = prop.getProperty("app.show-queries", "false");
		return showQueries.equals("true");
	}

	public String getFrontendBaseUrl() {
		return prop.getProperty("frontend.base-url", "http://localhost:8080/#");
	}

	public int getAPP_Port() {	
		return getOrDefault("app.port", 4567);
	}

	public int getAPP_WaitingTime() {
		return getOrDefault("app.waiting-time", 5*60);
	}
	public String getDB_Driver() {
		return prop.getProperty("db.driver", "mysql");
	}

	public String getDB_Host() {
		return prop.getProperty("db.host", "localhost");
	}

	public int getDB_Port() {
		return getOrDefault("db.port", 3306);
	}

	public String getDB_Database() {
		return prop.getProperty("db.database", "inprice");
	}

	public String getDB_Additions() {
		return prop.getProperty("db.additions", "");
	}

	public String getDB_Username() {
		return prop.getProperty("db.username", "root");
	}

	public String getDB_Password() {
		return prop.getProperty("db.password", "1234");
	}

	public int getAS_SaltRounds() {
		return getOrDefault("app.security.salt-rounds", 12);
	}

	public String getRedis_Host() {
		return prop.getProperty("redis.host", "localhost");
	}

	public int getRedis_Port() {
		return getOrDefault("redis.port", 6379);
	}

	public String getRedis_Password() {
		return prop.getProperty("redis.password", null);
	}

	public String getMQ_Host() {
		return prop.getProperty("mq.host", "localhost");
	}

	public int getMQ_Port() {
		return getOrDefault("mq.port", 5672);
	}

	public String getMQ_Username() {
		return prop.getProperty("mq.username", "guest");
	}

	public String getMQ_Password() {
		return prop.getProperty("mq.password", "guest");
	}

	public String getMQ_ChangeExchange() {
		return prop.getProperty("mq.exchange.change", "changes");
	}

	public String getRoutingKey_DeletedLinks() {
		return prop.getProperty("routingKey.for.deleted-links", "deleted-links");
	}

	public Long getTTL_Tokens() {
		return DateUtils.parseTimePeriodAsMillis(prop.getProperty("ttl.for.tokens", "15m"));
	}

	public String getEmail_Sender() {
		return prop.getProperty("email.sender", "support@inprice.io");
	}

	public String getEmail_APIKey() {
		return prop.getProperty("email.sendgrid.api-key", "test");
	}

	public String getPrefix_ForSearchingInEbay() {
		return prop.getProperty("prefix.for.searching.in.ebay", "https://www.ebay.com/itm/");
	}

	public String getPrefix_ForSearchingInAmazon() {
		return prop.getProperty("prefix.for.searching.in.amazon", "https://www.amazon.com/dp/");
	}

	private int getOrDefault(String key, int defauld) {
		String val = prop.getProperty(key, "" + defauld);
		return Integer.parseInt(val.trim());
	}

}
