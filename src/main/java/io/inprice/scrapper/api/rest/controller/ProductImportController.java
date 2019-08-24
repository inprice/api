package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.CSVUploadDTO;
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

import java.io.File;

import static spark.Spark.post;

public class ProductImportController {

    private static final Logger log = LoggerFactory.getLogger(ProductImportController.class);
    private static final ProductService productService = Beans.getSingleton(ProductService.class);

    @Routing
    public void routes() {

        //upload csv
        post(Consts.Paths.Product.UPLOAD_CSV, "multipart/form-data", (req, res) -> {
            ImportReport report = uploadCSV(req);
            res.status(report.getStatus());
            return report;
        }, Global.gson::toJson);

    }

    private ImportReport uploadCSV(Request req) {
        if (req.body().isEmpty()) {
            return InstantResponses.FILE_PROBLEM("CSV");
        }

        CSVUploadDTO csv = toModel(req);
        return productService.uploadCSV(csv);
    }

    private CSVUploadDTO toModel(Request req) {
        try {
            final String body = req.body();
            int firstSlash = body.indexOf("/");
            String fileName = body.substring(firstSlash, body.lastIndexOf("\""));
            final String bodyWithoutFile = body.replace(fileName, "").replace(",\"file\":\"\"", "");
            CSVUploadDTO csv = Global.gson.fromJson(bodyWithoutFile, CSVUploadDTO.class);
            csv.setFile(new File(fileName));
            return csv;
        } catch (Exception e) {
            log.error("Data conversion error for csv upload.", e);
        }

        return null;
    }

}
