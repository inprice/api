package io.inprice.scrapper.api.app.token;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RSetCache;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RedisClient;

public final class TokenRepository {

   private final RedisClient redisClient = Beans.getSingleton(RedisClient.class);
   private final RSetCache<String> invalidatedTokenSet = redisClient.getInvalidatedTokenSet();

   public void invalidateToken(TokenType tokenType, String token) {
      long expireAt = (System.currentTimeMillis() + tokenType.ttl());
      invalidatedTokenSet.add(token, expireAt, TimeUnit.MILLISECONDS);
   }

   public boolean isTokenInvalidated(String token) {
      return invalidatedTokenSet.contains(token);
   }

}
