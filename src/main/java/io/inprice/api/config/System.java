package io.inprice.api.config;

import com.google.gson.annotations.SerializedName;

import io.inprice.common.config.BaseSystem;

public class System extends BaseSystem {

	@SerializedName("webUrl")
	public String WEB_URL;

	@SerializedName("apiUrl")
	public String API_URL;

	@SerializedName("port")
	public int PORT;

	@SerializedName("saltRounds")
	public int SALT_ROUNDS;

	@SerializedName("freeUseDays")
	public int FREE_USE_DAYS;

	@SerializedName("requestExecutionThreshold")
	public int REQUEST_EXECUTION_THRESHOLD;

}
