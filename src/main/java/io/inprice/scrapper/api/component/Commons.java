package io.inprice.scrapper.api.component;

import java.util.List;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.javalin.http.Context;

public class Commons {

   public static ServiceResponse createResponse(Context ctx, ServiceResponse serviceResponse) {
      ctx.status(HttpStatus.OK_200);
      return serviceResponse;
   }

   public static ServiceResponse createResponse(List<String> problems) {
      if (problems.size() > 0) {
         return new ServiceResponse(problems);
      } else {
         return Responses.OK;
      }
   }

}
