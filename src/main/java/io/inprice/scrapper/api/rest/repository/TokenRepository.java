package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RedisClient;
import org.redisson.api.RSetCache;

import java.util.concurrent.TimeUnit;

public final class TokenRepository {

    private final RedisClient redisClient = Beans.getSingleton(RedisClient.class);
    private final Properties props = Beans.getSingleton(Properties.class);

    private final RSetCache<String> invalidatedTokenSet = redisClient.getInvalidatedTokenSet();

    public void invalidateToken(String token) {
        long ttlAsSeconds = props.getTTL_Tokens() / 1000;
        ttlAsSeconds += 60;

        invalidatedTokenSet.add(token, ttlAsSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenInvalidated(String token) {
        return invalidatedTokenSet.contains(token);
    }

}
