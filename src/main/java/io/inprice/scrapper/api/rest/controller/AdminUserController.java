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
import io.inprice.scrapper.api.rest.service.AdminUserService;
import io.inprice.scrapper.api.rest.service.AuthService;
import io.inprice.scrapper.api.rest.service.TokenService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    
    private static final AdminUserService adminUserService = Beans.getSingleton(AdminUserService.class);
    private static final TokenService tokenService = Beans.getSingleton(TokenService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.AdminUser.BASE, (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = upsert(authUser, req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.AdminUser.BASE, (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = upsert(authUser, req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.AdminUser.BASE + "s", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = getList(authUser);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/:id", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = toggleStatus(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password
        put(Consts.Paths.AdminUser.PASSWORD, (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = updatePassword(authUser, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(AuthUser authUser, Long id) {
        return adminUserService.findById(authUser, id);
    }

    private ServiceResponse getList(AuthUser authUser) {
        return adminUserService.getList(authUser);
    }

    private ServiceResponse deleteById(AuthUser authUser, Long id) {
        return adminUserService.deleteById(authUser, id);
    }

    private ServiceResponse toggleStatus(AuthUser authUser, Long id) {
        return adminUserService.toggleStatus(authUser, id);
    }

    private ServiceResponse upsert(AuthUser authUser, String body, boolean insert) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            if (insert)
                return adminUserService.insert(authUser, userDTO);
            else
                return adminUserService.update(authUser, userDTO, true);
        }
        log.error("Invalid user data: " + body);
        return InstantResponses.INVALID_DATA("user!");
    }

    private ServiceResponse updatePassword(AuthUser authUser, String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return adminUserService.updatePassword(authUser, passwordDTO);
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
