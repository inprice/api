package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminService;
import io.inprice.scrapper.api.rest.service.AuthService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.put;

public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final AdminService adminService = Beans.getSingleton(AdminService.class);
    private static final AuthService authService = Beans.getSingleton(AuthService.class);

    @Routing
    public void routes() {

        //update admin info
        put(Consts.Paths.Admin.BASE, (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);

            ServiceResponse serviceRes = update(authUser, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update admin password
        put(Consts.Paths.Admin.PASSWORD, (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);

            ServiceResponse serviceRes = updatePassword(authUser, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //set default workspace
        put(Consts.Paths.Admin.WORKSPACE + "/:ws_id", (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);
            final Long wsId = LongValidator.getInstance().validate(req.params(":ws_id"));

            ServiceResponse serviceRes = setDefaultWorkspace(authUser, wsId);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse update(AuthUser claims, String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            return adminService.update(claims, userDTO);
        }
        log.error("Invalid user data: " + body);
        return InstantResponses.INVALID_DATA("user!");
    }

    private ServiceResponse updatePassword(AuthUser claims, String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return adminService.updatePassword(claims, passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return InstantResponses.INVALID_DATA("password!");
    }

    private ServiceResponse setDefaultWorkspace(AuthUser claims, Long wsId) {
        return adminService.setDefaultWorkspace(claims, wsId);
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
