package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.service.DashboardService;
import org.eclipse.jetty.http.HttpStatus;

import static spark.Spark.get;

public class DashboardController {

    private static final DashboardService service = Beans.getSingleton(DashboardService.class);

    @Routing
    public void routes() {

        get(Consts.Paths.Misc.DASHBOARD, (req, res) -> {
            res.status(HttpStatus.OK_200);
            return service.getReport();
        }, Global.gson::toJson);

    }

}
