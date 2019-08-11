package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSet;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {

	private final Properties properties = Beans.getSingleton(Properties.class);

	private final RedissonClient client;
	private final RSetCache<String> invalidatedTokens;

	RedisClient() {
		final String redisPass = properties.getRedis_Password();

		Config config = new Config();
		config
			.useSingleServer()
			.setAddress(String.format("redis://%s:%d", properties.getRedis_Host(), properties.getRedis_Port()))
			.setPassword(! StringUtils.isBlank(redisPass) ? redisPass : null);

		client = Redisson.create(config);
		invalidatedTokens = client.getSetCache("TOKENS-INVALIDATED");
	}

	public RSetCache<String> getInvalidatedTokenSet() {
		return invalidatedTokens;
	}

	public void shutdown() {
		client.shutdown();
	}

}
