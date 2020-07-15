package io.inprice.api.external;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Global;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.config.SysProps;

public class RedisClient {

  private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

  private static boolean isHealthy;
  private static RedissonClient client;

  private static RSetCache<String> requestingEmailsSet;
  private static RMapCache<String, ForRedis> sessionsMap;
  private static RMapCache<String, Serializable> tokensMap;
  private static RMapCache<Long, Map<String, Object>> dashboardsMap;

  static {
    final String redisPass = SysProps.REDIS_PASSWORD();
    Config config = new Config();
    config
      .useSingleServer()
      .setAddress(String.format("redis://%s:%d", SysProps.REDIS_HOST(), SysProps.REDIS_PORT()))
      .setPassword(!StringUtils.isBlank(redisPass) ? redisPass : null)
      .setConnectionPoolSize(10)
      .setConnectionMinimumIdleSize(1)
      .setIdleConnectionTimeout(5000)
      .setTimeout(5000);

    while (!isHealthy && Global.isApplicationRunning) {
      try {
        client = Redisson.create(config);

        requestingEmailsSet = client.getSetCache("api:requesting:emails");
        sessionsMap = client.getMapCache("api:token:sessions");
        tokensMap = client.getMapCache("api:tokens");
        dashboardsMap = client.getMapCache("api:dashboards");
        isHealthy = true;
      } catch (Exception e) {
        log.error("Failed to connect to Redis server, trying again in 3 seconds! " + e.getMessage());
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ignored) { }
      }
    }

  }

  public static void shutdown() {
    if (client != null) client.shutdown();
    isHealthy = false;
  }

  public static ServiceResponse isEmailRequested(RateLimiterType type, String email) {
    boolean exists = requestingEmailsSet.contains(type.name() + email);
    if (exists) {
      return Responses.Already.REQUESTED_EMAIL;
    }
    requestingEmailsSet.add(type.name() + email, type.ttl(), TimeUnit.MILLISECONDS);
    return Responses.OK;
  }

  public static boolean removeRequestedEmail(RateLimiterType type, String email) {
    return requestingEmailsSet.remove(type.name() + email);
  }

  public static boolean addSesions(List<ForRedis> sessions) {
    for (ForRedis ses : sessions) {
      sessionsMap.put(ses.getHash(), ses);
    }
    return true;
  }

  public static ForRedis getSession(String hash) {
    return sessionsMap.get(hash);
  }

  public static void updateSessions(Map<String, ForRedis> map) {
    sessionsMap.putAll(map);
  }

  public static boolean removeSesion(String hash) {
    ForRedis ses = sessionsMap.remove(hash);
    return ses != null;
  }

  public static boolean refreshSesion(String hash) {
    ForRedis ses = sessionsMap.get(hash);
    if (ses != null) {
      ses.setAccessedAt(new Date());
      sessionsMap.put(ses.getHash(), ses);
      return true;
    }
    return false;
  }

  public static RMapCache<String, Serializable> getTokensMap() {
    return tokensMap;
  }

  public static RMapCache<Long, Map<String, Object>> getDashboardsMap() {
    return dashboardsMap;
  }

}
