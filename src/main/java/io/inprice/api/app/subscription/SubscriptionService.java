package io.inprice.api.app.subscription;

import io.inprice.api.info.ServiceResponse;
import io.inprice.common.helpers.Beans;

public class SubscriptionService {

  private static final SubscriptionRepository repository = Beans.getSingleton(SubscriptionRepository.class);

  public ServiceResponse getTransactions() {
    return repository.getTransactions();
  }

  public ServiceResponse cancel() {
    return repository.cancel();
  }

}
