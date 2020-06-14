package io.inprice.scrapper.api.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.helpers.ThreadPools;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.models.Competitor;

/**
 * Creates products by using competitors' link details
 */
public class ProductCreationFromLinkConsumer {

  private static final Logger log = LoggerFactory.getLogger(ProductCreationFromLinkConsumer.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

  public void start() {
    log.info("Product creation consumer is up and running.");

    final Channel channel = RabbitMQ.openChannel();

    final Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        ThreadPools.PRODUCT_CREATION_POOL.submit(() -> {
          boolean created = false;
          ServiceResponse res = Responses.NotFound.COMPETITOR;
          try {
            Competitor link = JsonConverter.fromJson(new String(body), Competitor.class);
            if (link != null) {
              res = productRepository.createFromLink(link);
              if (res.isOK()) {
                created = true;
              } else {
                log.error("DB problem while activating a competitor!");
              }
            } else {
              log.error("Product creation link is null!");
            }

          } catch (Exception e) {
            log.error("Failed to submit tasks into ThreadPool", e);
          }

          try {
            if (created) {
              channel.basicAck(envelope.getDeliveryTag(), false);
            } else {
              channel.basicNack(envelope.getDeliveryTag(), false, false);
            }
          } catch (IOException e1) {
            log.error("Failed to send a message to dlq", e1);
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_PRODUCT_CREATION_QUEUE(), false, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue for creating products from link.", e);
    }
  }

}
