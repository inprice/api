package io.inprice.api.token;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.CodeGenerator;

public class Tokens {

  private static final Random random = new Random();

  public static String add(TokenType tokenType, Serializable object) {
    String token = null;
    if (TokenType.REGISTRATION_REQUEST.equals(tokenType)) {
      token = generateNumericToken();
    } else {
      token = CodeGenerator.hash();
    }

    RedisClient.tokensMap.put(getKey(tokenType, token), object, tokenType.ttl(), TimeUnit.MILLISECONDS);
    return token;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Serializable> T get(TokenType tokenType, String token) {
    Serializable seri = RedisClient.tokensMap.get(getKey(tokenType, token));
    if (seri != null)
      return (T) seri;
    else
      return null;
  }

  public static boolean remove(TokenType tokenType, String token) {
    return (RedisClient.tokensMap.remove(getKey(tokenType, token)) != null);
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
