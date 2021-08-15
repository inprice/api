package io.inprice.api.external;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.Redis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 *
 * @since 2021-08-15
 * @author mdpinar
 */
public class RedisClient {

	private static final String SESSIONS_KEY = "sessions";
	
  public Response isEmailRequested(RateLimiterType type, String email) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	String key = getRequestedEmailKey(type, email);

    	String val = jedis.get(key);
    	if (val != null) {
    		return Responses.Already.REQUESTED_EMAIL;    		
    	}

    	jedis.set(key, "1"); //value doesn't matter here
    	jedis.expire(key, type.getTTL());
    	return Responses.OK;
    }
  }

  public boolean removeRequestedEmail(RateLimiterType type, String email) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	String key = getRequestedEmailKey(type, email);
  		return jedis.del(key) > 0;
    }
  }

  private static String getRequestedEmailKey(RateLimiterType type, String email) {
    return String.format("requested-emails:%s:%s", type.name(), email);
  }

  public boolean addSesions(List<ForRedis> sessions) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	Transaction trans = jedis.multi();
    	for (ForRedis ses : sessions) {
    		trans.hset(SESSIONS_KEY, ses.getHash(), JsonConverter.toJson(ses));
    	}
    	trans.exec();
    }
    return true;
  }

  public ForRedis getSession(String hash) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	String json = jedis.hget(SESSIONS_KEY, hash);
    	if (json != null) {
    		return JsonConverter.fromJson(json, ForRedis.class);
    	}
    }
    return null;
  }

  public void updateSessions(Map<String, ForRedis> map) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	Transaction trans = jedis.multi();
    	for (Entry<String, ForRedis> entry: map.entrySet()) {
    		trans.hset(SESSIONS_KEY, entry.getKey(), JsonConverter.toJson(entry.getValue()));
    	}
    	trans.exec();
    }
  }

  //TODO: adding a multiple version of this method would be nice!
  public void removeSesion(String hash) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	jedis.hdel(SESSIONS_KEY, hash);
    }
  }

  public boolean refreshSesion(String hash) {
    try (Jedis jedis = Redis.getPool().getResource()) {
    	String json = jedis.hget(SESSIONS_KEY, hash);
    	if (json != null) {
    		ForRedis ses = JsonConverter.fromJson(json, ForRedis.class);
    		ses.setAccessedAt(new Date());
    		jedis.hset(SESSIONS_KEY, hash, JsonConverter.toJson(ses));
    		return true;
    	}
    }
    return false;
  }

}
