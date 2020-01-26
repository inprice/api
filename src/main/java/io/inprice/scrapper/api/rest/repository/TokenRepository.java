package io.inprice.scrapper.api.rest.repository;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RSetCache;

import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RedisClient;

public final class TokenRepository {

    private final RedisClient redisClient = Beans.getSingleton(RedisClient.class);
    private final RSetCache<String> invalidatedTokenSet = redisClient.getInvalidatedTokenSet();

    public void invalidateToken(String token) {
        long ttlAsSeconds = (Props.getTTL_RefreshTokens() * 2) / 1000;
        invalidatedTokenSet.add(token, ttlAsSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenInvalidated(String token) {
        return invalidatedTokenSet.contains(token);
    }

}
