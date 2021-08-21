package io.inprice.api.publisher;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import io.inprice.api.config.Props;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.info.EmailData;

/**
 * @since 2021-08-19
 * @author mdpinar
 */
public class EmailPublisher {

  private static final Logger logger = LoggerFactory.getLogger(EmailPublisher.class);
	
	private static Connection conn;
	private static Channel channel;

	static {
		try {
			conn = RabbitMQ.createConnection("Api-PUB: " + Props.getConfig().QUEUES.SENDING_EMAILS.NAME);
			channel = conn.createChannel();
  	} catch (IOException e) {
      logger.error("Failed to establish RabbitMQ connection", e);
		}
	}

	public static void publish(EmailData emailData) {
  	try {
	  	String outMessage = JsonConverter.toJson(emailData);
	  	channel.basicPublish("", Props.getConfig().QUEUES.SENDING_EMAILS.NAME, null, outMessage.getBytes());
  	} catch (IOException e) {
      logger.error("Failed to publish email", e);
		}
	}
	
}
