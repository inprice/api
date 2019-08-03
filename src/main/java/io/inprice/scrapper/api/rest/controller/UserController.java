package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.UserService;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.put;

public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService service = Beans.getSingleton(UserService.class);

    private static final String ROOT = "user";

    @Routing
    public void routes() {

        //update
        //a user can edit only his/her data
        put(ROOT + "/update", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = update(Consts.claims, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password
        put(ROOT + "/update-password", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = updatePassword(Consts.claims, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    Response update(Claims claims, String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            return service.update(claims, userDTO);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    Response updatePassword(Claims claims, String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return service.updatePassword(claims, passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for password!");
    }

    private UserDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, UserDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for user!", e);
        }

        return null;
    }

}
