package io.inprice.scrapper.api.external;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import io.inprice.scrapper.api.app.auth.UserSession;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;

public class RedisClient {

   private static final RedissonClient client;

   private static final RSetCache<String> limitedIpsSet;
   private static final RMapCache<String, UserSession> sessionMap;

   static {
      final String redisPass = Props.getRedis_Password();

      Config config = new Config();
      config.useSingleServer().setAddress(String.format("redis://%s:%d", Props.getRedis_Host(), Props.getRedis_Port()))
            .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null);

      client = Redisson.create(config);
      limitedIpsSet = client.getSetCache("api:limited:ips");
      sessionMap = client.getMapCache("api:token:sessions");
   }

   public static ServiceResponse isIpRateLimited(RateLimiterType type, String ip) {
      boolean exists = limitedIpsSet.contains(type.name() + ip);
      if (exists) {
         return Responses.Illegal.TOO_MUCH_REQUEST;
      }
      limitedIpsSet.add(type.name() + ip, type.ttl(), TimeUnit.MILLISECONDS);
      return Responses.OK;
   }

   public static boolean addSesion(UserSession session) {
      sessionMap.put(session.getTokenHash(), session);
      return true;
   }

   public static UserSession getSession(String md5Hash) {
      return sessionMap.get(md5Hash);
   }

   public static boolean removeSesion(String md5Hash) {
      UserSession ses = sessionMap.remove(md5Hash);
      return ses != null;
   }

   public static boolean refreshSesion(String md5Hash) {
      UserSession session = sessionMap.get(md5Hash);
      if (session != null) {
         session.setAccessedAt(new Date());
         addSesion(session);
         return true;
      }
      return false;
   }

   public static RedissonClient getClient() {
      return client;
   }

   public static void shutdown() {
      client.shutdown();
   }

}
