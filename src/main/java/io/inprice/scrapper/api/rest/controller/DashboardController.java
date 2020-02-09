package io.inprice.scrapper.api.rest.controller;

import static spark.Spark.get;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.DashboardService;

public class DashboardController {

    private static final DashboardService service = Beans.getSingleton(DashboardService.class);

    @Routing
    public void routes() {

        get(Consts.Paths.Misc.DASHBOARD, (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            res.status(HttpStatus.OK_200);
            return service.getReport();
        }, Global.gson::toJson);

    }

}
