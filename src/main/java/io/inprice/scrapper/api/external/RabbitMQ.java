package io.inprice.scrapper.api.external;

import java.io.Serializable;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Global;

public class RabbitMQ {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQ.class);

  private static boolean isHealthy;
  private static Channel channel;

  public static Channel getChannel() {
    if (!isChannelActive()) {
      synchronized (log) {
        if (!isChannelActive()) {
          final ConnectionFactory connFactory = new ConnectionFactory();
          connFactory.setHost(Props.MQ_HOST());
          connFactory.setPort(Props.MQ_PORT());
          connFactory.setUsername(Props.MQ_USERNAME());
          connFactory.setPassword(Props.MQ_PASSWORD());
          connFactory.setAutomaticRecoveryEnabled(true);

          while (!isHealthy && Global.isApplicationRunning) {
            try {
              final String deletedLinksQueue = Props.MQ_ROUTING_DELETED_LINKS();
              Connection conn = connFactory.newConnection();
              channel = conn.createChannel();

              channel.exchangeDeclare(Props.MQ_EXCHANGE_CHANGES(), "topic");
              channel.queueDeclare(deletedLinksQueue, true, false, false, null);
              channel.queueBind(deletedLinksQueue, Props.MQ_EXCHANGE_CHANGES(), deletedLinksQueue + ".#");
              isHealthy = true;
              log.error("Connected to RabbitMQ server and checked all the exchanges and queues");
            } catch (Exception e) {
              log.error("Failed to connect to RabbitMQ server, trying again in 3 seconds! " + e.getMessage());
              try {
                Thread.sleep(3000);
              } catch (InterruptedException ignored) {
              }
            }
          }
        }
      }
    }

    return channel;
  }

  public static boolean publish(String routingKey, Serializable message) {
    return publish(Props.MQ_EXCHANGE_CHANGES(), routingKey, message);
  }

  public static boolean publish(String exchange, String routingKey, Serializable message) {
    try {
      getChannel().basicPublish(exchange, routingKey, null, SerializationUtils.serialize(message));
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
