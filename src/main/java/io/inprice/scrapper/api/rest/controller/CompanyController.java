package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.post;
import static spark.Spark.put;

public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private static final CompanyService companyService = Beans.getSingleton(CompanyService.class);

    @Routing
    public void routes() {

        put(Consts.Paths.Company.BASE, (req, res) -> {
            ServiceResponse serviceRes = update(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Company.REGISTER, (req, res) -> {
            ServiceResponse serviceRes = insert(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse insert(String body) {
        CompanyDTO companyDTO = toModel(body);
        if (companyDTO != null) {
            return companyService.insert(companyDTO);
        }
        return InstantResponses.INVALID_DATA("company!");
    }

    private ServiceResponse update(String body) {
        CompanyDTO companyDTO = toModel(body);
        if (companyDTO != null) {
            return companyService.update(companyDTO);
        }
        return InstantResponses.INVALID_DATA("company!");
    }

    private CompanyDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, CompanyDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for company, body: " + body);
        }

        return null;
    }

}
