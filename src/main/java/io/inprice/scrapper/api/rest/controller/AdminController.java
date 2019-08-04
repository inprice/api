package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.AdminService;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.put;

public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final String ROOT = "admin";
    private final AdminService service = Beans.getSingleton(AdminService.class);

    @Routing
    public void routes() {

        //update admin info
        put(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = update(Consts.ADMIN_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update admin password
        put(ROOT + "/password", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = updatePassword(Consts.ADMIN_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //set default workspace
        put(ROOT + "/workspace/:ws_id", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Long wsId = LongValidator.getInstance().validate(req.params(":ws_id"));
            Response serviceRes = setDefaultWorkspace(Consts.ADMIN_CLAIMS, wsId);
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

    Response setDefaultWorkspace(Claims claims, Long wsId) {
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
