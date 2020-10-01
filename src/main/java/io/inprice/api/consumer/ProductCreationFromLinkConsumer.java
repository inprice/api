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
              res = createFromLink(link);
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



  public Response createFromLink(Competitor link) {
    Connection con = null;
    try {
      con = db.getTransactionalConnection();

      ProductDTO dto = new ProductDTO();
      dto.setCode(link.getSku());
      dto.setName(link.getName());
      dto.setBrandId(lookupRepository.add(con, LookupType.BRAND, link.getBrand()).getId());
      dto.setPrice(link.getPrice());
      dto.setCompanyId(link.getCompanyId());

      boolean isCompelted = false;

      Response result = insertANewProduct(con, dto);
      if (result.isOK()) {
        isCompelted = db.executeQuery(String.format("delete from competitor where id=%d", link.getId()),
            String.format("Failed to delete link to be product. Id: %d", link.getId()));
      }

      if (isCompelted) {
        db.commit(con);
        return Responses.OK;
      } else {
        db.rollback(con);
        return result;
      }

    } catch (Exception e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to insert a new product. " + link.toString(), e);
      return Responses.ServerProblem.EXCEPTION;
    } finally {
      if (con != null)
        db.close(con);
    }
  }


 */  }

}
