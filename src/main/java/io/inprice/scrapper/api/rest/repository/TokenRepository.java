package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RedisClient;
import org.redisson.api.RSetCache;

import java.util.concurrent.TimeUnit;

public final class TokenRepository {

    private final RedisClient redisClient = Beans.getSingleton(RedisClient.class);
    private final Properties properties = Beans.getSingleton(Properties.class);

    private final RSetCache<String> invalidatedTokenSet = redisClient.getInvalidatedTokenSet();

    public void invalidateToken(String token) {
        invalidatedTokenSet.add(token, properties.getTTL_InvalidatedTokens(), TimeUnit.MINUTES);
    }

    public boolean isTokenInvalidated(String token) {
        return invalidatedTokenSet.contains(token);
    }

}
