package io.inprice.scrapper.api.helpers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class RabbitMQ {

	private static final Logger log = LoggerFactory.getLogger(RabbitMQ.class);
	private static final Properties props = Beans.getSingleton(Properties.class);

	private static Channel channel;

	public static Channel getChannel() {
		if (!isChannelActive()) {
			synchronized (log) {
				if (!isChannelActive()) {
					final ConnectionFactory connectionFactory = new ConnectionFactory();
					connectionFactory.setHost(props.getMQ_Host());
					connectionFactory.setPort(props.getMQ_Port());
					connectionFactory.setUsername(props.getMQ_Username());
					connectionFactory.setPassword(props.getMQ_Password());

					try {
						final String deletedLinksQueue = props.getRoutingKey_DeletedLinks();

						Connection connection = connectionFactory.newConnection();
						channel = connection.createChannel();

						channel.exchangeDeclare(props.getMQ_ChangeExchange(), "topic");
						channel.queueDeclare(deletedLinksQueue, true, false, false, null);
						channel.queueBind(deletedLinksQueue, props.getMQ_ChangeExchange(), deletedLinksQueue + ".#");
					} catch (Exception e) {
						log.error("Error in opening RabbitMQ channel", e);
					}
				}
			}
		}

		return channel;
	}

	public static boolean publish(String routingKey, Serializable message) {
		return publish(props.getMQ_ChangeExchange(), routingKey, message);
	}

	public static boolean publish(String exchange, String routingKey, Serializable message) {
		try {
			getChannel().basicPublish(exchange, routingKey, null, Converter.fromObject(message));
			return true;
		} catch (Exception e) {
			log.error("Failed to send a message to queue", e);
			return false;
		}
	}

	public static void closeChannel() {
		try {
			if (isChannelActive()) {
				channel.close();
			}
		} catch (Exception e) {
			log.error("Error while RabbitMQ.channel is closed.", e);
		}
	}

	public static boolean isChannelActive() {
		return (channel != null && channel.isOpen());
	}

}
