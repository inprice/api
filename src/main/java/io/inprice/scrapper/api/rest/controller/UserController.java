package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.UserService;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.put;

public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final String ROOT = "user";
    private final UserService service = Beans.getSingleton(UserService.class);

    @Routing
    public void routes() {

        //update. a user can edit only his/her data
        put(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            ServiceResponse serviceRes = update(Consts.USER_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password. a user can edit only his/her password
        put(ROOT + "/password", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            ServiceResponse serviceRes = updatePassword(Consts.USER_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //set default workspace
        put(ROOT + "/workspace/:ws_id", (req, res) -> {
            Long wsId = LongValidator.getInstance().validate(req.params(":ws_id"));
            ServiceResponse serviceRes = setDefaultWorkspace(Consts.ADMIN_CLAIMS, wsId);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    ServiceResponse update(AuthUser claims, String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            return service.update(claims, userDTO);
        }
        log.error("Invalid user data: " + body);
        return new ServiceResponse(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    ServiceResponse updatePassword(AuthUser claims, String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return service.updatePassword(claims, passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return new ServiceResponse(HttpStatus.BAD_REQUEST_400, "Invalid data for password!");
    }

    ServiceResponse setDefaultWorkspace(AuthUser claims, Long wsId) {
        return service.setDefaultWorkspace(claims, wsId);
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
