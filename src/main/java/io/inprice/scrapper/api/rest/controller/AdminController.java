package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.put;

public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final AdminService adminService = Beans.getSingleton(AdminService.class);

    @Routing
    public void routes() {

        //update admin info
        put(Consts.Paths.Admin.BASE, (req, res) -> {
            ServiceResponse serviceRes = update(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update admin password
        put(Consts.Paths.Admin.PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = updatePassword(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse update( String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            return adminService.update(userDTO);
        }
        log.error("Invalid user data: " + body);
        return InstantResponses.INVALID_DATA("user!");
    }

    private ServiceResponse updatePassword(String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return adminService.updatePassword(passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return InstantResponses.INVALID_DATA("password!");
    }

    private UserDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, UserDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for user, body: " + body);
        }

        return null;
    }

}
