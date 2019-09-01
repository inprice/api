package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.ProductCodeImportService;
import io.inprice.scrapper.api.rest.service.ProductCSVImportService;
import io.inprice.scrapper.api.rest.service.ProductImportService;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.ImportProduct;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;

import static spark.Spark.*;

public class ProductImportController {

    private static final ProductImportService importService = Beans.getSingleton(ProductImportService.class);
    private static final ProductCSVImportService csvImportService = Beans.getSingleton(ProductCSVImportService.class);
    private static final ProductCodeImportService asinImportService = Beans.getSingleton(ProductCodeImportService.class);

    @Routing
    public void routes() {

        //find
        get(Consts.Paths.Product.IMPORT_BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Product.IMPORT_BASE + "s", (req, res) -> {
            ServiceResponse serviceRes = getList();
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Product.IMPORT_BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //upload csv
        post(Consts.Paths.Product.IMPORT_CSV, "text/csv", (req, res) -> {
            ImportProduct importReport = uploadCSV(req);
            res.status(importReport.getStatus());
            return importReport;
        }, Global.gson::toJson);

        //upload ebay SKU list
        post(Consts.Paths.Product.IMPORT_EBAY_SKU_LIST, "text/plain", (req, res) -> {
            ImportProduct importReport = uploadCodeList(ImportType.EBAY_SKU, req);
            res.status(importReport.getStatus());
            return importReport;
        }, Global.gson::toJson);

        //upload amazon ASIN list
        post(Consts.Paths.Product.IMPORT_AMAZON_ASIN_LIST, "text/plain", (req, res) -> {
            ImportProduct importReport = uploadCodeList(ImportType.AMAZON_ASIN,req);
            res.status(importReport.getStatus());
            return importReport;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(Long id) {
        return importService.findById(id);
    }

    private ServiceResponse getList() {
        return importService.getList();
    }

    private ServiceResponse deleteById(Long id) {
        if (Context.getAuthUser().getType().equals(UserType.READER)) return InstantResponses.PERMISSION_PROBLEM("delete an import!");
        return importService.deleteById(id);
    }

    private ImportProduct uploadCSV(Request req) {
        ImportProduct result = new ImportProduct();

        if (Context.getAuthUser().getType().equals(UserType.READER)) {
            result.setStatus(HttpStatus.FORBIDDEN_403);
            result.setResult("User has no permission to import any product!");
            return result;
        }

        if (req.body().isEmpty()) {
            result.setStatus(HttpStatus.BAD_REQUEST_400);
            result.setResult("CSV file is empty!");
        } else {
            result = csvImportService.upload(req.body());
        }

        return result;
    }

    private ImportProduct uploadCodeList(ImportType importType, Request req) {
        ImportProduct result = new ImportProduct();

        if (Context.getAuthUser().getType().equals(UserType.READER)) {
            result.setStatus(HttpStatus.FORBIDDEN_403);
            result.setResult("User has no permission to import any product!");
            return result;
        }

        if (req.body().isEmpty()) {
            result.setStatus(HttpStatus.BAD_REQUEST_400);
            result.setResult((ImportType.EBAY_SKU.equals(importType) ? "SKU" : "ASIN") + " list is empty!");
        } else {
            result = asinImportService.upload(importType, req.body());
        }

        return result;
    }

}
