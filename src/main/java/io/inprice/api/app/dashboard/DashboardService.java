package io.inprice.api.app.dashboard;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.inprice.api.consts.Responses;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;

public class DashboardService {

  private final DashboardRepository repository = Beans.getSingleton(DashboardRepository.class);

  public Response getReport(boolean refresh) {
    Map<String, Object> data = null;

    if (! refresh) data = RedisClient.getDashboardsMap().get(CurrentUser.getCompanyId());
    if (data == null) data = repository.getReport();

    if (data != null) {
      RedisClient.getDashboardsMap().put(CurrentUser.getCompanyId(), data, 5, TimeUnit.MINUTES);
      return new Response(data);
    }

    return Responses.DataProblem.DB_PROBLEM;
  }

}
