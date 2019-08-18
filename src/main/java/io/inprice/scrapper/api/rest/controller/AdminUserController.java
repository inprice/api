package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminUserService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    
    private static final AdminUserService adminUserService = Beans.getSingleton(AdminUserService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.AdminUser.BASE, (req, res) -> {
            ServiceResponse serviceRes = upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.AdminUser.BASE, (req, res) -> {
            ServiceResponse serviceRes = upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.AdminUser.BASE + "s", (req, res) -> {
            ServiceResponse serviceRes = getList();
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = toggleStatus(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password
        put(Consts.Paths.AdminUser.PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = updatePassword(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(Long id) {
        return adminUserService.findById(id);
    }

    private ServiceResponse getList() {
        return adminUserService.getList();
    }

    private ServiceResponse deleteById(Long id) {
        return adminUserService.deleteById(id);
    }

    private ServiceResponse toggleStatus(Long id) {
        return adminUserService.toggleStatus(id);
    }

    private ServiceResponse upsert(String body, boolean insert) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            if (insert)
                return adminUserService.insert(userDTO);
            else
                return adminUserService.update(userDTO, true);
        }
        log.error("Invalid user data: " + body);
        return InstantResponses.INVALID_DATA("user!");
    }

    private ServiceResponse updatePassword(String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return adminUserService.updatePassword(passwordDTO);
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
