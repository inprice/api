package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.service.ProductCSVImportService;
import io.inprice.scrapper.common.models.ImportProduct;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;

import static spark.Spark.post;

public class ProductImportController {

    private static final ProductCSVImportService csvImportService = Beans.getSingleton(ProductCSVImportService.class);

    @Routing
    public void routes() {

        //upload csv
        post(Consts.Paths.Product.IMPORT_CSV, "text/csv", (req, res) -> {
            ImportProduct imbort = uploadCSV(req);
            res.status(imbort.getStatus());
            return imbort;
        }, Global.gson::toJson);

    }

    private ImportProduct uploadCSV(Request req) {
        ImportProduct result = new ImportProduct();
        if (req.body().isEmpty()) {
            result.setStatus(HttpStatus.BAD_REQUEST_400);
            result.setResult("CSV file is empty!");
        } else {
            result = csvImportService.upload(req.body());
        }

        return result;
    }

}
