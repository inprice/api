package io.inprice.api.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.QueueDef;

public class Queues {

	@JsonProperty("sendingEmails")
	public QueueDef SENDING_EMAILS;
	
}
