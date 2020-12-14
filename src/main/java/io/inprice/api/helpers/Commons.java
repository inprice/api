package io.inprice.api.helpers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForResponse;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.common.models.Company;
import io.javalin.http.Context;

public class Commons {

  public static Response createResponse(Context ctx, Response serviceResponse) {
    ctx.status(HttpStatus.OK_200);
    return serviceResponse;
  }

  public static Response refreshSession(Company company) {
    return refreshSession(company, company.getStatus(), company.getRenewalAt());
  }

  public static Response refreshSession(Company company, CompanyStatus status, Date renewalAt) {
    company.setStatus(status);
    company.setRenewalAt(renewalAt);
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
