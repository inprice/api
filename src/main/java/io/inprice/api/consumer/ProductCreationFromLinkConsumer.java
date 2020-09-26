package io.inprice.api.consumer;

/**
 * Creates products by using competitors' link details
 */
public class ProductCreationFromLinkConsumer {

  public void start() {
    //TODO: bu işlemlerin tamamı Manager projesinde commondao kullanılarak yapılacak
/*     log.info("Product creation consumer is up and running.");

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
 */  }

}
