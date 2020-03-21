package io.inprice.scrapper.api.app.dashboard;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;

public class DashboardService {

   private final DashboardRepository repository = Beans.getSingleton(DashboardRepository.class);

   public ServiceResponse getReport() {
      // TODO: a caching mechanism with TTL must be added here
      return new ServiceResponse(repository.getReport());
   }

}
