package io.inprice.api.app.utils;

import java.lang.reflect.Field;
import java.util.Map;

import io.inprice.api.Application;
import io.inprice.api.consts.Global;
import io.inprice.common.helpers.Database;
import kong.unirest.Unirest;
import redis.embedded.RedisServer;

public class TestHelper {

	public static void initTestServers() {
		injectEnvironmentVariable("APP_ENV", "TEST");
		injectEnvironmentVariable("KEY_ENCRYPTION", "f7*$q{>AKbC<B)!s@n7=P");
		injectEnvironmentVariable("KEY_SUPER_USER", ",~2w&RWmV3bchk']pKbC<B)!:F[*#Ss8");
		injectEnvironmentVariable("KEY_USER", "-8'fq{>As@n77jcx24.U*$=PS]#Z5wY+");

		Unirest.config().defaultBaseUrl("http://localhost:4567");

		RedisServer redisServer = new RedisServer(6379);
		redisServer.start();

		Application.main(null);
		while (!Global.isApplicationRunning) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		
		Database.cleanTestTables();
	}

	/**
	 * https://blog.sebastian-daschner.com/entries/changing_env_java
	 * 
	 */
	@SuppressWarnings("unchecked")
	private static void injectEnvironmentVariable(String key, String value) {
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

}
