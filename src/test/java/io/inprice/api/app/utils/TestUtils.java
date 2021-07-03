package io.inprice.api.app.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Map;

import io.inprice.api.Application;
import io.inprice.api.consts.Global;
import io.inprice.common.helpers.BaseRedisClient;
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
	
	public static void setup() {
		if (isPortAvailable(SERVER_PORT)) {
  		setSystemEnvVariable("APP_ENV", "TEST");
  		setSystemEnvVariable("KEY_ENCRYPTION", "f7*$q{>AKbC<B)!s@n7=P");
  		setSystemEnvVariable("KEY_SUPER_USER", ",~2w&RWmV3bchk']pKbC<B)!:F[*#Ss8");
  		setSystemEnvVariable("KEY_USER", "-8'fq{>As@n77jcx24.U*$=PS]#Z5wY+");
  		
  		Unirest.config().defaultBaseUrl("http://localhost:" + SERVER_PORT);

  		if (isPortAvailable(6379)) {
  			RedisServer redisServer = new RedisServer(6379);
    		redisServer.start();
  		}

  		Application.main(null);
  		while (!Global.isApplicationRunning) {
  			try {
  				Thread.sleep(1000);
  			} catch (InterruptedException e) {
  			}
  		}

		} else {
			//redis must be cleaned up before starting any test
			BaseRedisClient redisClient = new BaseRedisClient();
			redisClient.open(() -> redisClient.getClient().getKeys().flushall());
		}
		Database.cleanTestTables();
	}
	
	public static Cookies login(TestRole role) {
		return login(TestAccount.X, role);
	}

	/**
	 * Logs in for the given role of the given account and returns cookies.
	 * 
	 * @param forAccount X, Y and Z...
	 * @param role any role is ok
	 * @return authorized cookies
	 */
	public static Cookies login(TestAccount forAccount, TestRole role) {
		JSONObject user = null;
		switch (role) {
  		case SUPER: {
  			user = Fixtures.SUPER_USER;
  			break;
  		}
  		case BANNED: {
  			user = Fixtures.BANNED_USER;
  			break;
  		}
  		default: {
  			user = Fixtures.NORMAL_USER(role, forAccount);
  			break;
  		}
		}
		HttpResponse<?> res = 
			Unirest.post("/login")
  			.body(user)
  			.asEmpty();
		return res.getCookies();
	}

	public static HttpResponse<?> logout(Cookies cookies) {
		return Unirest.post("/logout").cookie(cookies).asEmpty();
	}

	/**
	 * Sets System env variables which are immutable by default
	 * 
	 * https://blog.sebastian-daschner.com/entries/changing_env_java
	 */
	@SuppressWarnings("unchecked")
	private static void setSystemEnvVariable(String key, String value) {
		try {
  		Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
  
  		Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
  		Object unmodifiableMap = unmodifiableMapField.get(null);
  		injectIntoUnmodifiableMap(key, value, unmodifiableMap);
  
  		Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
  		Map<String, String> map = (Map<String, String>) mapField.get(null);
  		map.put(key, value);
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	private static Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field;
	}

	@SuppressWarnings("unchecked")
	private static void injectIntoUnmodifiableMap(String key, String value, Object map)
	    throws ReflectiveOperationException {

		Class<?> unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
		Field field = getAccessibleField(unmodifiableMap, "m");
		Object obj = field.get(map);
		((Map<String, String>) obj).put(key, value);
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
