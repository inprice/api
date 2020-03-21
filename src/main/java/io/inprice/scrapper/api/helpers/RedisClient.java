package io.inprice.scrapper.api.helpers;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.meta.RateLimiterType;

public class RedisClient {

   private static final RedissonClient client;

   private static final RSetCache<String> invalidatedTokens;
   private static final RSetCache<String> rateLimitingSet;

   static {
      final String redisPass = Props.getRedis_Password();

      Config config = new Config();
      config.useSingleServer().setAddress(String.format("redis://%s:%d", Props.getRedis_Host(), Props.getRedis_Port()))
            .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null);

      client = Redisson.create(config);
      invalidatedTokens = client.getSetCache("api:invalidated:tokens");
      rateLimitingSet = client.getSetCache("api:rate-limiting:ips");
   }

   public static boolean invalidateToken(TokenType tokenType, String token) {
      return invalidatedTokens.add(token, System.currentTimeMillis() + tokenType.ttl(), TimeUnit.MILLISECONDS);
   }

   public static boolean addIpToRateLimiter(RateLimiterType type, String ip) {
      return rateLimitingSet.add(type.name() + ip, System.currentTimeMillis() + type.ttl(), TimeUnit.MILLISECONDS);
   }

   public static boolean isIpRateLimited(RateLimiterType type, String ip) {
      return rateLimitingSet.contains(type.name() + ip);
   }

   public static boolean isTokenInvalidated(String token) {
      return invalidatedTokens.contains(token);
   }

   public static void shutdown() {
      client.shutdown();
   }

}
