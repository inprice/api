package io.inprice.api.app.imbort;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.common.meta.ImportType;

public interface ImportService {

  default Response upload( String content) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

  default Response upload(ImportType importType, String content) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

}