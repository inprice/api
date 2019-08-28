package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ImportReport;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.rest.service.ProductCSVImportService;
import org.apache.http.entity.ContentType;
import spark.Request;

import static spark.Spark.post;

public class ProductImportController {

    private static final ProductCSVImportService csvImportService = Beans.getSingleton(ProductCSVImportService.class);

    @Routing
    public void routes() {

        //upload csv
        post(Consts.Paths.Product.UPLOAD_CSV, "text/csv", (req, res) -> {
            ImportReport report = uploadCSV(req);
            res.status(report.getStatus());
            return report;
        }, Global.gson::toJson);

    }

    private ImportReport uploadCSV(Request req) {
        if (req.body().isEmpty()) {
            return InstantResponses.FILE_PROBLEM("CSV");
        }
        return csvImportService.upload(req.body());
    }

}
