package io.inprice.scrapper.api.helpers;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {

   private static final RedissonClient client;

   private static final RSetCache<String> invalidatedTokens;
   private static final RSetCache<String> forgotPasswordEmails;

   static {
      final String redisPass = Props.getRedis_Password();

      Config config = new Config();
      config.useSingleServer().setAddress(String.format("redis://%s:%d", Props.getRedis_Host(), Props.getRedis_Port()))
            .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null);

      client = Redisson.create(config);
      invalidatedTokens = client.getSetCache("inprice:tokens:invalidated");
      forgotPasswordEmails = client.getSetCache("inprice:emails:forgotten");
   }

   public RSetCache<String> getInvalidatedTokenSet() {
      return invalidatedTokens;
   }

   public static RSetCache<String> getForgotPasswordEmails() {
      return forgotPasswordEmails;
   }

   public static void shutdown() {
      client.shutdown();
   }

}
