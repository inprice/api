package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.UserService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.put;

public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final UserService userService = Beans.getSingleton(UserService.class);

    @Routing
    public void routes() {

        //update. a user can edit only his/her data
        put(Consts.Paths.User.BASE, (req, res) -> {
            ServiceResponse serviceRes = update(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password. a user can edit only his/her password
        put(Consts.Paths.User.PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = updatePassword(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //set default workspace
        put(Consts.Paths.User.WORKSPACE + "/:ws_id", (req, res) -> {
            final Long wsId = LongValidator.getInstance().validate(req.params(":ws_id"));

            ServiceResponse serviceRes = setDefaultWorkspace(wsId);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse update(String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            return userService.update(userDTO);
        }
        log.error("Invalid user data: " + body);
        return InstantResponses.INVALID_DATA("user!");
    }

    private ServiceResponse updatePassword(String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return userService.updatePassword(passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return InstantResponses.INVALID_DATA("password!");
    }

    private ServiceResponse setDefaultWorkspace(Long wsId) {
        return userService.setDefaultWorkspace(wsId);
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
