package io.inprice.api.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.BaseConfig;

public class Config extends BaseConfig {

	@JsonProperty("app")
	public App APP;

	@JsonProperty("keys")
	public Keys KEYS;

	@JsonProperty("ttls")
	public Ttls TTLS;

	@JsonProperty("thresholds")
	public Thresholds THRESHOLDS;

	@JsonProperty("queues")
	public Queues QUEUES;

}
