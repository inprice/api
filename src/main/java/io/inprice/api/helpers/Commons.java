package io.inprice.api.helpers;

import java.util.List;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.javalin.http.Context;

public class Commons {

  public static Response createResponse(Context ctx, Response serviceResponse) {
    ctx.status(HttpStatus.OK_200);
    return serviceResponse;
  }

  public static Response createResponse(List<String> problems) {
    if (problems.size() > 0) {
      return new Response(problems);
    } else {
      return Responses.OK;
    }
  }

}
