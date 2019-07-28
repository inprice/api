package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.CompanyService;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.post;
import static spark.Spark.put;

public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyService service = Beans.getSingleton(CompanyService.class);

    private static final String ROOT = "company";

    @Routing
    public void routes() {
        post(ROOT, (req, res) -> {
            Response serviceRes = upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT, (req, res) -> {
            Response serviceRes = upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);
    }

    Response findById(Long id) {
        return service.findById(id);
    }

    Response upsert(String body, boolean insert) {
        CompanyDTO companyDTO = toModel(body);
        if (companyDTO != null) {
            if (insert)
                return service.insert(companyDTO);
            else
                return service.update(companyDTO);
        }
        log.error("Invalid company data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for company!");
    }

    private CompanyDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, CompanyDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for company!", e);
        }

        return null;
    }

}
