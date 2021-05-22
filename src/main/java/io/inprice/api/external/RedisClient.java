package io.inprice.api.external;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RMapCache;
import org.redisson.api.RQueue;
import org.redisson.api.RSetCache;
import org.redisson.api.RTopic;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.BaseRedisClient;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.models.analytics.AccessLog;

public class RedisClient {

  private static BaseRedisClient baseClient;
  private static RTopic linkStatusChangeTopic;

  private static RSetCache<String> requestingEmailsSet;
  private static RMapCache<String, ForRedis> sessionsMap;

  public static RMapCache<String, Serializable> tokensMap;
  public static RQueue<AccessLog> userLogQueue;

  static {
    baseClient = new BaseRedisClient();
    baseClient.open(() -> {
      linkStatusChangeTopic = baseClient.getClient().getTopic(SysProps.REDIS_STATUS_CHANGE_TOPIC);

      requestingEmailsSet = baseClient.getClient().getSetCache("api:requesting:emails");
      sessionsMap = baseClient.getClient().getMapCache("api:token:sessions");
      tokensMap = baseClient.getClient().getMapCache("api:tokens");
      userLogQueue = baseClient.getClient().getQueue("api:buffer:userlog");
    });
  }

  public static Response isEmailRequested(RateLimiterType type, String email) {
    if (!SysProps.APP_ENV.equals(AppEnv.PROD)) return Responses.OK;

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

  public static void removeSesion(String hash) {
    sessionsMap.removeAsync(hash);
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

  public static void addUserLog(AccessLog userLog) {
  	userLog.setCreatedAt(new Date());
  	userLogQueue.add(userLog);
  }
  
  public static void shutdown() {
    linkStatusChangeTopic.removeAllListeners();
    baseClient.shutdown();
  }

}
