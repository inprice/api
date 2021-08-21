package io.inprice.api.config;

import com.google.gson.annotations.SerializedName;

import io.inprice.common.config.QueueDef;

public class Queues {

	@SerializedName("sendingEmails")
	public QueueDef SENDING_EMAILS;
	
}
