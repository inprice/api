package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {

	private static final Properties props = Beans.getSingleton(Properties.class);

	private static final RedissonClient client;
	
	private static final RSetCache<String> invalidatedTokens;
	private static final RSetCache<String> forgotPasswordEmails;

	static {
		final String redisPass = props.getRedis_Password();

		Config config = new Config();
		config
			.useSingleServer()
			.setAddress(String.format("redis://%s:%d", props.getRedis_Host(), props.getRedis_Port()))
			.setPassword(! StringUtils.isBlank(redisPass) ? redisPass : null);

		client = Redisson.create(config);
		invalidatedTokens = client.getSetCache("TOKENS-INVALIDATED");
		forgotPasswordEmails = client.getSetCache("EMAILS-FORGOTTEN");
	}

	public RSetCache<String> getInvalidatedTokenSet() {
		return invalidatedTokens;
	}

	public static RSetCache<String> getForgotpasswordemails() {
		return forgotPasswordEmails;
	}

	public static void shutdown() {
		client.shutdown();
	}

}
