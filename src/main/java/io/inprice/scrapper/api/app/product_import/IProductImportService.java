package io.inprice.scrapper.api.app.product_import;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

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