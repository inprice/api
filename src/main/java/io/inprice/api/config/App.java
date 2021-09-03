package io.inprice.api.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.BaseSystem;

public class App extends BaseSystem {

	@JsonProperty("webUrl")
	public String WEB_URL;

	@JsonProperty("apiUrl")
	public String API_URL;

	@JsonProperty("port")
	public int PORT;

	@JsonProperty("saltRounds")
	public int SALT_ROUNDS;

	@JsonProperty("freeUseDays")
	public int FREE_USE_DAYS;

}
