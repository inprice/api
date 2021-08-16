package io.inprice.api.token;

import java.util.Random;

import io.inprice.api.helpers.CodeGenerator;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.Redis;
import redis.clients.jedis.Jedis;

public class Tokens {
	
  private static final Random random = new Random();

  public static String add(TokenType tokenType, Object object) {
    String token = null;
    if (TokenType.REGISTRATION_REQUEST.equals(tokenType)) {
      token = generateNumericToken();
    } else {
      token = CodeGenerator.hash();
    }

    try (Jedis jedis = Redis.getPool().getResource()) {
    	String key = getKey(tokenType, token);
    	jedis.set(key, JsonConverter.toJson(object));
    	jedis.expire(key, tokenType.getTTL());
    }

    return token;
  }

  public static <T> T get(TokenType tokenType, String token, Class<T> expectedClass) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	String key = getKey(tokenType, token);
    	String json = jedis.get(key);
    	if (json != null) {
    		return (T) JsonConverter.fromJson(json, expectedClass);
    	} else {
    		return null;
    	}
    }
  }

  public static boolean remove(TokenType tokenType, String token) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	String key = getKey(tokenType, token);
    	long result = jedis.del(key);
    	return (result > 0);
    }
  }

  private static String getKey(TokenType tokenType, String token) {
    return tokenType.name() + ":" + token;
  }

  /**
   * Generates five digits random number
   * then adds a checksum at the end.
   * 
   * @return 6-digit randomized token
   */
  private static String generateNumericToken() {
    String token = ""+(random.nextInt(89999) + 10000);

    int total = 0;
    for (int i = 0; i < 5; i++) {
      total += token.charAt(i) * ((i == 0 || i % 2 == 0) ? 7 : 5);
    }

    return token+(total%10);
  }
 
}
