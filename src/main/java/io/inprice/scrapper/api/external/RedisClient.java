package io.inprice.scrapper.api.external;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
import io.inprice.scrapper.api.session.info.ForRedis;

public class RedisClient {

   private static RedissonClient client;

   private static RSetCache<String> limitedIpsSet;
   private static RMapCache<String, ForRedis> sessionMap;

   static {
      final String redisPass = Props.getRedis_Password();

      Config config = new Config();
      config
         .useSingleServer()
         .setAddress(String.format("redis://%s:%d", Props.getRedis_Host(), Props.getRedis_Port()))
         .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null);

      client = Redisson.create(config);
      client.getNodesGroup().pingAll();
      limitedIpsSet = client.getSetCache("api:limited:ips");
      sessionMap = client.getMapCache("api:token:sessions");
   }

   public static void shutdown() {
      client.shutdown();
   }

   public static ServiceResponse isIpRateLimited(RateLimiterType type, String ip) {
      boolean exists = limitedIpsSet.contains(type.name() + ip);
      if (exists) {
         return Responses.Illegal.TOO_MUCH_REQUEST;
      }
      limitedIpsSet.add(type.name() + ip, type.ttl(), TimeUnit.MILLISECONDS);
      return Responses.OK;
   }

   public static boolean addSesions(List<ForRedis> sessions) {
      for (ForRedis ses: sessions) {
         sessionMap.put(ses.getHash(), ses);
      }
      return true;
   }

   public static ForRedis getSession(String hash) {
      return sessionMap.get(hash);
   }

   public static boolean removeSesion(String hash) {
      ForRedis ses = sessionMap.remove(hash);
      return ses != null;
   }

   public static boolean refreshSesion(String hash) {
      ForRedis ses = sessionMap.get(hash);
      if (ses != null) {
         ses.setAccessedAt(new Date());
         sessionMap.put(ses.getHash(), ses);
         return true;
      }
      return false;
   }

   public static RedissonClient getClient() {
      return client;
   }

}
