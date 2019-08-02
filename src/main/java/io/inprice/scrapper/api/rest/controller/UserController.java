package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.UserService;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService service = Beans.getSingleton(UserService.class);

    private static final String ROOT = "user";

    @Routing
    public void routes() {
        get(ROOT + "/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = update(Consts.claims.getCompanyId(), req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    Response findById(Long id) {
        return service.findById(id);
    }

    Response update(long companyId, String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            userDTO.setCompanyId(companyId); //
            return service.update(userDTO);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
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
