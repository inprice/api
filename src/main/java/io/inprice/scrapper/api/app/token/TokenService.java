package io.inprice.scrapper.api.app.token;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMapCache;

import io.inprice.scrapper.api.external.RedisClient;

public class TokenService {

   private static final RMapCache<String, Serializable> tokensMap;

   static {
      tokensMap = RedisClient.getClient().getMapCache("api:tokens");
   }

   public static String add(TokenType tokenType, Serializable object) {
      final String token = generateToken();
      tokensMap.put(getKey(tokenType, token), object, tokenType.ttl(), TimeUnit.MILLISECONDS);
      return token;
   }

   @SuppressWarnings("unchecked")
   public static <T extends Serializable> T get(TokenType tokenType, String token) {
      if (isTokenValid(token)) {
         Serializable seri = tokensMap.get(getKey(tokenType, token));
         if (seri != null) return (T) seri;
      }
      return null;
   }

   public static boolean remove(TokenType tokenType, String token) {
      return (tokensMap.remove(getKey(tokenType, token)) != null);
   }

   private static String getKey(TokenType tokenType, String token) {
      return tokenType.name() + "-" + token;
   }

   private static String generateToken() {
      return UUID.randomUUID().toString();
   }

   private static final String SAMPLE_TOKEN = generateToken();

   public static boolean isTokenValid(String token) {
      if (StringUtils.isNotBlank(token) && token.length() == SAMPLE_TOKEN.length()) {
         try {
            UUID.fromString(token);
            return true;
         } catch (IllegalArgumentException ignored) { }
      }
      return false;
   }

}
