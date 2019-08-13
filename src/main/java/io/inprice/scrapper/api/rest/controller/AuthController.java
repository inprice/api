package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AuthService;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import static spark.Spark.get;
import static spark.Spark.post;

public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final AuthService authService = Beans.getSingleton(AuthService.class);

    @Routing
    public void routes() {

        post(Consts.Paths.Intro.LOGIN, (req, res) -> {
            ServiceResponse serviceRes = login(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Intro.REFRESH_TOKEN, (req, res) -> {
            ServiceResponse serviceRes = refresh(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Intro.FORGOT_PASSWORD, (req, res) -> {
            //todo: to be implemented
            res.status(HttpStatus.OK_200);
            return new ServiceResponse(HttpStatus.OK_200);
        }, Global.gson::toJson);

        get(Consts.Paths.Intro.RESET_PASSWORD, (req, res) -> {
            //todo: to be implemented
            res.status(HttpStatus.OK_200);
            return new ServiceResponse(HttpStatus.OK_200);
        }, Global.gson::toJson);

        post(Consts.Paths.Intro.LOGOUT, (req, res) -> {
            ServiceResponse serviceRes = logout(req);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse login(Request req, Response res) {
        LoginDTO loginDTO = toLoginModel(req.body());
        if (loginDTO != null) {
            return authService.login(loginDTO, res);
        }
        return InstantResponses.INVALID_PARAM("Email or password");
    }

    private ServiceResponse refresh(Request req, Response res) {
        return authService.refresh(req, res);
    }

    private ServiceResponse logout(Request req) {
        authService.revokeToken(authService.getToken(req));
        return InstantResponses.OK;
    }

    private LoginDTO toLoginModel(String body) {
        try {
            return Global.gson.fromJson(body, LoginDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for login form, body: " + body);
        }

        return null;
    }

}
