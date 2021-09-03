package io.inprice.api.utils;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

import io.inprice.api.Application;
import io.inprice.api.config.Props;
import io.inprice.common.helpers.Database;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import redis.embedded.RedisServer;

/**
 * Provides functions for testing
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
public class TestUtils {
	
	private static final int SERVER_PORT = 4567;
	
	private static final StringBuilder sqlScripts;
	
	static {
		sqlScripts = new StringBuilder();
		try {
			String baseDir = "db/fixtures/";
			List<String> files = IOUtils.readLines(TestUtils.class.getClassLoader().getResourceAsStream(baseDir), StandardCharsets.UTF_8);
			for (String file: files) {
				sqlScripts.append(IOUtils.toString(TestUtils.class.getClassLoader().getResourceAsStream(baseDir+file), StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setup() {
		if (isPortAvailable(SERVER_PORT)) {
  		Unirest.config().defaultBaseUrl("http://localhost:" + SERVER_PORT);

  		if (isPortAvailable(6379)) {
  			RedisServer redisServer = new RedisServer(6379);
    		redisServer.start();
  		}

  		Application.main(null);
		}
		Database.cleanDBForTests(sqlScripts.toString(), Props.getConfig().APP.ENV);
	}

	/**
	 * Logs in for the given role of the given account and returns cookies.
	 * 
	 * @param forAccount X, Y and Z...
	 * @param role any role is ok
	 * @return authorized cookies
	 */
	public static Cookies login(JSONObject loginUser) {
		assertNotNull(loginUser);
		HttpResponse<?> res = Unirest.post("/login").body(loginUser).asEmpty();
		return res.getCookies();
	}

	public static HttpResponse<?> logout(Cookies cookies) {
		return Unirest.post("/logout").cookie(cookies).asEmpty();
	}

	/**
	 * Checks to see if a specific port is available.
	 * 
	 * https://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
	 */
	public static boolean isPortAvailable(int port) {
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}
		return false;
	}

}
