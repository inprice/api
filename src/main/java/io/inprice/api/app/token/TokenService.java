package io.inprice.api.app.token;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.inprice.api.external.RedisClient;

public class TokenService {

   public static String add(TokenType tokenType, Serializable object) {
      final String token = generateToken();
      RedisClient.getTokensMap().put(getKey(tokenType, token), object, tokenType.ttl(), TimeUnit.MILLISECONDS);
      return token;
   }

   @SuppressWarnings("unchecked")
   public static <T extends Serializable> T get(TokenType tokenType, String token) {
      Serializable seri = RedisClient.getTokensMap().get(getKey(tokenType, token));
      if (seri != null)
         return (T) seri;
      else
         return null;
   }

   public static boolean remove(TokenType tokenType, String token) {
      return (RedisClient.getTokensMap().remove(getKey(tokenType, token)) != null);
   }

   private static String getKey(TokenType tokenType, String token) {
      return tokenType.name() + ":" + token;
   }

   private static String generateToken() {
      return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
   }

}
