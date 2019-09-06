package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminService;
import org.apache.commons.lang3.StringUtils;

import static spark.Spark.put;

public class AdminController {

    private static final AdminService service = Beans.getSingleton(AdminService.class);

    @Routing
    public void routes() {

        //update admin info
        put(Consts.Paths.Admin.BASE, (req, res) -> {
            ServiceResponse serviceRes = service.update(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update admin password
        put(Consts.Paths.Admin.PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = service.updatePassword(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private UserDTO toModel(String body) {
        if (! StringUtils.isBlank(body)) {
            try {
                return Global.gson.fromJson(body, UserDTO.class);
            } catch (Exception e) {
                //
            }
        }
        return null;
    }

}
