package io.inprice.api.config;

import com.google.gson.annotations.SerializedName;

public class Thresholds {

	@SerializedName("responseTimeLatency")
	public int RESPONSE_TIME_LATENCY;

	@SerializedName("accessLogRowLimit")
	public int ACCESS_LOG_ROW_LIMIT;
	
}
