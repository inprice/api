package io.inprice.api.consumer;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.product.ProductRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.ThreadPools;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.models.Competitor;

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
          ServiceResponse res = Responses.NotFound.COMPETITOR;
          try {
            Competitor link = JsonConverter.fromJson(new String(body), Competitor.class);
            if (link != null) {
              res = productRepository.createFromLink(link);
              if (! res.isOK()) {
                log.error("DB problem while activating a competitor!");
              }
            } else {
              log.error("Product creation link is null!");
            }

          } catch (Exception e) {
            log.error("Failed to create product from link", e);
          }
        });
      }
    };

    try {
      channel.basicConsume(SysProps.MQ_PRODUCT_CREATION_QUEUE(), true, consumer);
    } catch (IOException e) {
      log.error("Failed to set a queue for creating products from link.", e);
    }
  }

}
