package io.inprice.api.app.product_import;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.meta.ImportType;

/**
 * IProductImportService
 */
public interface IProductImportService {

  default ServiceResponse upload( String content) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

  default ServiceResponse upload(ImportType importType, String content) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

}