package io.inprice.api.external;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.Rabbit;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.QueueName;

/**
 *
 * @since 2021-08-15
 * @author mdpinar
 */
public class RabbitClient {

  private static final Logger logger = LoggerFactory.getLogger(RabbitClient.class);
	
	private static ExecutorService tPool = Executors.newFixedThreadPool(1);
	private static Channel channel;

	static {
  	try {
  		Connection connection = Rabbit.createConnection("Api publisher: " + QueueName.SENDING_EMAILS.getName());
			channel = connection.createChannel();
		} catch (IOException e) {
			logger.error("Failed to connect rabbitmq", e);
		}
	}
	
  public static void sendEmail(EmailData emailData) {
  	tPool.submit(() -> {
    	try {
    		String outMessage = JsonConverter.toJson(emailData);
				channel.basicPublish("", QueueName.SENDING_EMAILS.getName(), null, outMessage.getBytes());
			} catch (IOException e) {
				logger.error("Failed to publish email data to rabbitmq", e);
			}
  	});
  }

}
