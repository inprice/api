package io.inprice.scrapper.api.helpers;

import org.apache.commons.lang3.RandomStringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

public class RedisClient {

	private static final RedissonClient client;
	private static final RSetCache<String> tokenSet;
	private static final RSetCache<String> apiKeySet;

	static {
		Config config = new Config();
		config
				.useSingleServer()
				.setAddress(String.format("redis://%s:%d", io.inprice.crawler.common.config.Config.REDIS_HOST, io.inprice.crawler.common.config.Config.REDIS_PORT))
				.setPassword(io.inprice.crawler.common.config.Config.REDIS_PASSWORD);

		client = Redisson.create(config);

		tokenSet = client.getSetCache(Consts.CSRF_TOKEN + "S");
		apiKeySet = client.getSetCache(Consts.API_KEY + "S");
	}

	public static String createCSRFToken() {
		final StringBuilder token = new StringBuilder();
		token.append(RandomStringUtils.randomAlphanumeric(8));
		token.append("-");
		token.append(RandomStringUtils.randomAlphanumeric(4));
		token.append("-");
		token.append(RandomStringUtils.randomAlphanumeric(4));
		token.append("-");
		token.append(RandomStringUtils.randomAlphanumeric(4));
		token.append("-");
		token.append(RandomStringUtils.randomAlphanumeric(12));

		final String strToken = token.toString();
		tokenSet.add(strToken, io.inprice.crawler.common.config.Config.REDIS_TTL_HOURS_FOR_TOKENS, TimeUnit.HOURS);
		
		return strToken;
	}

	public static boolean hasCSRFToken(String token) {
		return tokenSet.contains(token);
	}

	public static void addApiKey(String apiKey) {
		apiKeySet.add(apiKey, io.inprice.crawler.common.config.Config.REDIS_TTL_HOURS_FOR_TOKENS, TimeUnit.HOURS);
	}

	public static boolean hasApiKey(String apiKey) {
		return apiKeySet.contains(apiKey);
	}

	public static void removeToken(String token) {
		tokenSet.remove(token);
	}

	public static void removeApiKey(String apiKey) {
		apiKeySet.remove(apiKey);
	}

	public static void shutdown() {
		client.shutdown();
	}

}
