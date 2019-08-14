package io.inprice.scrapper.api.config;

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
				log.error("Unable to find config.properties in class path!");
				return;
			}
			prop.load(input);
		} catch (IOException e) {
			log.error("Error", e);
		}
	}

	public boolean isRunningForTests() {
		String runningAt = prop.getProperty("app.running-at", "prod");
		return runningAt.equals("test");
	}

	public int getAPP_Port() {
		return getOrDefault("app.port", 4567);
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

	public int getTTL_TokensInSeconds() {
		return getOrDefault("ttl.tokens.in-seconds", 900); //15 minutes
	}

	public String getEmail_Sender() {
		return prop.getProperty("email.sender", "support@inprice.io");
	}

	public String getEmail_APIKey() {
		return prop.getProperty("email.sendgrid.api-key", "test");
	}

	private int getOrDefault(String key, int defauld) {
		String val = prop.getProperty(key, "" + defauld);
		return Integer.parseInt(val.trim());
	}

}
