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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import static spark.Spark.get;
import static spark.Spark.post;

public class IntroController {

    private static final Logger log = LoggerFactory.getLogger(IntroController.class);

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

    //todo: the logic here should be moved into the service
    private ServiceResponse login(Request req, Response res) {
        LoginDTO loginDTO = toLoginModel(req.body());
        if (loginDTO != null) {
            ServiceResponse<AuthUser> serviceRes = authService.login(loginDTO);
            if (serviceRes.isOK()) {
                res.header(Consts.Auth.AUTHORIZATION_HEADER, Consts.Auth.TOKEN_PREFIX + authService.newToken(serviceRes.getModel()));
                return InstantResponses.OK;
            }
            return serviceRes;
        }
        return InstantResponses.INVALID_PARAM("Email or password");
    }

    //todo: the logic here should be moved into the service
    private ServiceResponse refresh(Request req, Response res) {
        ServiceResponse serRes = new ServiceResponse(HttpStatus.UNAUTHORIZED_401);

        final String token = authService.getToken(req);

        if (StringUtils.isBlank(token)) {
            serRes.setResult("Missing header: " + Consts.Auth.AUTHORIZATION_HEADER);
        } else if (authService.isTokenInvalidated(token)) {
            serRes.setResult("Invalid token!");
        } else {
            authService.revokeToken(token);
            AuthUser authUser = authService.getAuthUser(token);
            res.header(Consts.Auth.AUTHORIZATION_HEADER, Consts.Auth.TOKEN_PREFIX + authService.newToken(authUser));

            serRes.setStatus(HttpStatus.OK_200);
            serRes.setResult("OK");
        }

        return serRes;
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
