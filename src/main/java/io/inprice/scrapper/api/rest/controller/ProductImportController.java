package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.ProductCSVImportService;
import io.inprice.scrapper.api.rest.service.ProductCodeImportService;
import io.inprice.scrapper.api.rest.service.ProductImportService;
import io.inprice.scrapper.api.rest.service.ProductURLImportService;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.models.ImportProduct;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class ProductImportController {

    private static final ProductImportService importService = Beans.getSingleton(ProductImportService.class);
    private static final ProductCSVImportService csvImportService = Beans.getSingleton(ProductCSVImportService.class);
    private static final ProductURLImportService urlImportService = Beans.getSingleton(ProductURLImportService.class);
    private static final ProductCodeImportService asinImportService = Beans.getSingleton(ProductCodeImportService.class);

    @Routing
    public void routes() {

        //find
        get(Consts.Paths.Product.IMPORT_BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, importService.findById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Product.IMPORT_BASE + "s", (req, res) -> {
            return Commons.createResponse(res, importService.getList());
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Product.IMPORT_BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, importService.deleteById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //upload csv
        post(Consts.Paths.Product.IMPORT_CSV, "text/csv", (req, res) -> {
            return createResponse(res, uploadCSV(req));
        }, Global.gson::toJson);

        //upload URL list
        post(Consts.Paths.Product.IMPORT_URL_LIST, "text/csv", (req, res) -> {
            return createResponse(res, uploadURL(req));
        }, Global.gson::toJson);

        //upload ebay SKU list
        post(Consts.Paths.Product.IMPORT_EBAY_SKU_LIST, "text/plain", (req, res) -> {
            return createResponse(res,  uploadCodeList(ImportType.EBAY_SKU, req));
        }, Global.gson::toJson);

        //upload amazon ASIN list
        post(Consts.Paths.Product.IMPORT_AMAZON_ASIN_LIST, "text/plain", (req, res) -> {
            return createResponse(res,  uploadCodeList(ImportType.AMAZON_ASIN, req));
        }, Global.gson::toJson);

    }

    private ImportProduct uploadCSV(Request req) {
        ImportProduct result = new ImportProduct();

        if (StringUtils.isBlank(req.body())) {
            result.setStatus(Responses.Invalid.EMPTY_FILE.getStatus());
        } else {
            result = csvImportService.upload(req.body());
        }

        return result;
    }

    private ImportProduct uploadURL(Request req) {
        ImportProduct result = new ImportProduct();

        if (StringUtils.isBlank(req.body())) {
            result.setStatus(Responses.Invalid.EMPTY_FILE.getStatus());
        } else {
            result = urlImportService.upload(req.body());
        }

        return result;
    }

    private ImportProduct uploadCodeList(ImportType importType, Request req) {
        ImportProduct result = new ImportProduct();

        if (StringUtils.isBlank(req.body())) {
            result.setStatus(Responses.Invalid.EMPTY_FILE.getStatus());
        } else {
            result = asinImportService.upload(importType, req.body());
        }

        return result;
    }

    private static ImportProduct createResponse(Response res, ImportProduct importReport) {
        res.status(importReport.getStatus() == 0 ? HttpStatus.OK_200 : HttpStatus.BAD_REQUEST_400);
        return importReport;
    }

}
