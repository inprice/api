package io.inprice.api.config;

import com.google.gson.annotations.SerializedName;

import io.inprice.common.config.BaseConfig;

public class Config extends BaseConfig {

	@SerializedName("app")
	public System APP;

	@SerializedName("keys")
	public Keys KEYS;

	@SerializedName("ttls")
	public Ttls TTLS;

	@SerializedName("intervals")
	public Intervals INTERVALS;

	@SerializedName("queues")
	public Queues QUEUES;

}
