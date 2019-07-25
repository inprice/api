package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.UserService;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.User;
import org.eclipse.jetty.http.HttpStatus;

public class UserController {

    private static final Logger log = new Logger(UserController.class);

    private final UserService service = Beans.getSingleton(UserService.class);

    public Response findById(Long id) {
        return service.findById(id);
    }

    public Response findByEmail(String email) {
        return service.findByEmail(email);
    }

    public Response upsert(String body, boolean insert) {
        User user = toModel(body);
        if (user != null) {
            if (insert)
                return service.insert(user);
            else
                return service.update(user);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid user data for user!");
    }

    private User toModel(String body) {
        User user;
        try {
            user = Global.gson.fromJson(body, User.class);
            if (user != null) return user;
        } catch (Exception e) {
            log.error("Data conversion error for user!", e);
        }

        return null;
    }

}
