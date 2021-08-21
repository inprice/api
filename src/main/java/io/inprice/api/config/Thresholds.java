package io.inprice.api.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Thresholds {

	@JsonProperty("responseTimeLatency")
	public int RESPONSE_TIME_LATENCY;

	@JsonProperty("accessLogRowLimit")
	public int ACCESS_LOG_ROW_LIMIT;
	
}
