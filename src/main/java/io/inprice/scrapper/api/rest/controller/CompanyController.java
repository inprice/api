package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import static spark.Spark.post;
import static spark.Spark.put;

public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);
    private static final CompanyService service = Beans.getSingleton(CompanyService.class);

    @Routing
    public void routes() {

        post(Consts.Paths.Auth.REGISTER, (req, res) -> {
            return Commons.createResponse(res, service.insert(toCompanyModel(req), res));
        }, Global.gson::toJson);

        put(Consts.Paths.Company.BASE, (req, res) -> {
            return Commons.createResponse(res, service.update(toCompanyModel(req)));
        }, Global.gson::toJson);

    }

    private CompanyDTO toCompanyModel(Request req) {
        try {
            return Global.gson.fromJson(req.body(), CompanyDTO.class);
        } catch (Exception e) {
            log.error("IP: {} -> Data conversion error for company. " + req.body(), req.ip());
        }
        return null;
    }

}
