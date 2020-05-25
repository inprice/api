package io.inprice.scrapper.api.app.dashboard;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class DashboardService {

  private final DashboardRepository repository = Beans.getSingleton(DashboardRepository.class);

  public ServiceResponse getReport(boolean refresh) {
    Map<String, Object> data = null;

    if (! refresh) data = RedisClient.getDashboardsMap().get(CurrentUser.getCompanyId());
    if (data == null) data = repository.getReport();

    if (data != null) {
      RedisClient.getDashboardsMap().put(CurrentUser.getCompanyId(), data, 5, TimeUnit.MINUTES);
      return new ServiceResponse(data);
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

}
