package io.inprice.api.config;

import com.google.gson.annotations.SerializedName;

import io.inprice.common.config.BaseConfig;

public class Config extends BaseConfig {

	@SerializedName("app")
	public App APP;

	@SerializedName("keys")
	public Keys KEYS;

	@SerializedName("ttls")
	public Ttls TTLS;

	@SerializedName("thresholds")
	public Thresholds THRESHOLDS;

	@SerializedName("queues")
	public Queues QUEUES;

}
