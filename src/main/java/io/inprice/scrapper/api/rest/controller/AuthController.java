package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AuthService;
import io.inprice.scrapper.api.rest.service.TokenService;
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
    private static final TokenService tokenService = Beans.getSingleton(TokenService.class);

    @Routing
    public void routes() {

        post(Consts.Paths.Auth.LOGIN, (req, res) -> {
            ServiceResponse serviceRes = login(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.REFRESH_TOKEN, (req, res) -> {
            ServiceResponse serviceRes = refresh(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.FORGOT_PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = forgotPassword(req);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.RESET_PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = resetPassword(req);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.LOGOUT, (req, res) -> {
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
        return InstantResponses.INVALID_DATA("email or password!");
    }

    private ServiceResponse refresh(Request req, Response res) {
        return authService.refresh(req, res);
    }

    private ServiceResponse forgotPassword(Request req) {
        EmailDTO emailDTO = toEmailModel(req.body());
        if (emailDTO != null) {
            return authService.forgotPassword(emailDTO);
        }
        return InstantResponses.INVALID_DATA("email!");
    }

    private ServiceResponse resetPassword(Request req) {
        PasswordDTO passwordDTO = toPasswordModel(req.body());
        if (passwordDTO != null) {
            return authService.resetPassword(passwordDTO);
        }
        return InstantResponses.INVALID_DATA("password!");
    }

    private ServiceResponse logout(Request req) {
        tokenService.revokeToken(tokenService.getToken(req));
        return InstantResponses.OK;
    }

    private PasswordDTO toPasswordModel(String body) {
        try {
            return Global.gson.fromJson(body, PasswordDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for password form, body: " + body);
        }

        return null;
    }

    private EmailDTO toEmailModel(String body) {
        try {
            return Global.gson.fromJson(body, EmailDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for email form, body: " + body);
        }

        return null;
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
