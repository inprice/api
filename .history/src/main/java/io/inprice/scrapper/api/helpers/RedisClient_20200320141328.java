package io.inprice.scrapper.api.helpers;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import io.inprice.scrapper.api.app.token.TokenType;

public class RedisClient {

   private static final RedissonClient client;

   private static final RSetCache<String> invalidatedTokens;
   private static final RSetCache<String> forgotPasswordRequesterIps;

   static {
      final String redisPass = Props.getRedis_Password();

      Config config = new Config();
      config.useSingleServer().setAddress(String.format("redis://%s:%d", Props.getRedis_Host(), Props.getRedis_Port()))
            .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null);

      client = Redisson.create(config);
      invalidatedTokens = client.getSetCache("api:invalidated:tokens");
      forgotPasswordRequesterIps = client.getSetCache("api:forgot-pass:requester:ips");
   }

   public static boolean invalidateToken(TokenType tokenType, String token) {
      return invalidatedTokens.add(token, tokenType.ttl(), TimeUnit.MILLISECONDS);
   }

   public static boolean addRequesterIpForForgotPass(String ip) {
      return forgotPasswordRequesterIps.add(ip, TokenType.PASSWORD_RESET.ttl(), TimeUnit.MILLISECONDS);
   }

   public static boolean isTokenInvalidated(String token) {
      return invalidatedTokens.contains(token);
   }

   public static boolean hasAlreadyRequestedForgotPassById(String ip) {
      return forgotPasswordRequesterIps.contains(ip);
   }

   public static void shutdown() {
      client.shutdown();
   }

}
