package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ImportReport;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.rest.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import static spark.Spark.post;

public class ProductImportController {

    private static final Logger log = LoggerFactory.getLogger(ProductImportController.class);
    private static final ProductService productService = Beans.getSingleton(ProductService.class);

    @Routing
    public void routes() {

        //upload csv
        post(Consts.Paths.Product.UPLOAD_CSV, "text/csv", (req, res) -> {
            ImportReport report = uploadCSV(req);
            res.status(report.getStatus());
            return report;
        }, Global.gson::toJson);

        //upload Amazon ASIN list
        post(Consts.Paths.Product.UPLOAD_AMAZON_ASIN_LIST, "text/plain", (req, res) -> {
            ImportReport report = uploadAmazonASIN(req);
            res.status(report.getStatus());
            return report;
        }, Global.gson::toJson);

    }

    private ImportReport uploadAmazonASIN(Request req) {
        if (req.body().isEmpty()) {
            return InstantResponses.FILE_PROBLEM("Amazon ASIN list");
        }
        return productService.uploadAmazonASIN(req.body());
    }

    private ImportReport uploadCSV(Request req) {
        if (req.body().isEmpty()) {
            return InstantResponses.FILE_PROBLEM("CSV");
        }
        return productService.uploadCSV(req.body());
    }

}
