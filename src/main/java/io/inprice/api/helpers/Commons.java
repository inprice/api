package io.inprice.api.helpers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForResponse;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Company;
import io.javalin.http.Context;

public class Commons {

  public static Response createResponse(Context ctx, Response serviceResponse) {
    ctx.status(HttpStatus.OK_200);
    return serviceResponse;
  }

  public static Response refreshSession(Long companyId) {
    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      return refreshSession(companyDao, companyId);
    }
  }

  public static Response refreshSession(CompanyDao companyDao, Long companyId) {
    Company company = companyDao.findById(companyId);
    return refreshSession(company);
  }

  public static Response refreshSession(Company company) {
    ForResponse session = new ForResponse(
      company,
      CurrentUser.getUserName(),
      CurrentUser.getEmail(),
      CurrentUser.getRole(),
      CurrentUser.getUserTimezone()
    );
    Map<String, Object> dataMap = new HashMap<>(1);
    dataMap.put("session", session);

    return new Response(dataMap);
  }

}
