package io.inprice.scrapper.api.helpers;

import java.io.Serializable;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQ {

   private static final Logger log = LoggerFactory.getLogger(RabbitMQ.class);

   private static Channel channel;

   public static Channel getChannel() {
      if (!isChannelActive()) {
         synchronized (log) {
            if (!isChannelActive()) {
               final ConnectionFactory connectionFactory = new ConnectionFactory();
               connectionFactory.setHost(Props.getMQ_Host());
               connectionFactory.setPort(Props.getMQ_Port());
               connectionFactory.setUsername(Props.getMQ_Username());
               connectionFactory.setPassword(Props.getMQ_Password());

               try {
                  final String deletedLinksQueue = Props.getRoutingKey_DeletedLinks();

                  Connection connection = connectionFactory.newConnection();
                  channel = connection.createChannel();

                  channel.exchangeDeclare(Props.getMQ_ChangeExchange(), "topic");
                  channel.queueDeclare(deletedLinksQueue, true, false, false, null);
                  channel.queueBind(deletedLinksQueue, Props.getMQ_ChangeExchange(), deletedLinksQueue + ".#");
               } catch (Exception e) {
                  log.error("Error in opening RabbitMQ channel", e);
               }
            }
         }
      }

      return channel;
   }

   public static boolean publish(String routingKey, Serializable message) {
      return publish(Props.getMQ_ChangeExchange(), routingKey, message);
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
