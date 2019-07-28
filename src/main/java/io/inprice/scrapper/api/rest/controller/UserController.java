package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.UserService;
import io.inprice.scrapper.common.models.User;
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
        get(ROOT + "/by-id/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        get(ROOT + "/by-email/:email", (req, res) -> {
            Response serviceRes = findByEmail(req.params(":email"));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(ROOT, (req, res) -> {
            Response serviceRes = upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT, (req, res) -> {
            Response serviceRes = upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);
    }

    Response findById(Long id) {
        return service.findById(id);
    }

    Response findByEmail(String email) {
        return service.findByEmail(email);
    }

    Response upsert(String body, boolean insert) {
        User user = toModel(body);
        if (user != null) {
            if (insert)
                return service.insert(user);
            else
                return service.update(user);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    private User toModel(String body) {
        try {
            return Global.gson.fromJson(body, User.class);
        } catch (Exception e) {
            log.error("Data conversion error for user!", e);
        }

        return null;
    }

}
