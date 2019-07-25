package io.inprice.scrapper.api.config;

import io.inprice.scrapper.common.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

	private static final Logger log = new Logger(Config.class);

	private final Properties prop;

	Config() {
		prop = new Properties();

		try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			if (input == null) {
				log.error("Unable to find config.properties in class path!");
				return;
			}
			prop.load(input);
		} catch (IOException e) {
			log.error(e);
		}
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

	public String getDB_Username() {
		return prop.getProperty("db.username", "root");
	}

	public String getDB_Password() {
		return prop.getProperty("db.password", "1234");
	}

	private int getOrDefault(String key, int defauld) {
		String val = prop.getProperty(key, "" + defauld);
		return Integer.parseInt(val.trim());
	}

}
