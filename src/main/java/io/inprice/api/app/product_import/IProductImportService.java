package io.inprice.api.app.product_import;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.common.meta.ImportType;

/**
 * IProductImportService
 */
public interface IProductImportService {

  default Response upload( String content) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

  default Response upload(ImportType importType, String content) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

}