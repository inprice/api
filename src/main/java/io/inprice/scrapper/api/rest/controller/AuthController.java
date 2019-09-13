package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import static spark.Spark.get;
import static spark.Spark.post;

public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final AuthService service = Beans.getSingleton(AuthService.class);

    @Routing
    public void routes() {

        post(Consts.Paths.Auth.LOGIN, (req, res) -> {
            ServiceResponse serviceRes = service.login(toLoginModel(req), res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        get(Consts.Paths.Auth.REFRESH_TOKEN, (req, res) -> {
            ServiceResponse serviceRes = service.refresh(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.FORGOT_PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = service.forgotPassword(toEmailModel(req));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.RESET_PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = service.resetPassword(toPasswordModel(req));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.LOGOUT, (req, res) -> {
            ServiceResponse serviceRes = service.logout(req);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private PasswordDTO toPasswordModel(Request req) {
        try {
            return Global.gson.fromJson(req.body(), PasswordDTO.class);
        } catch (Exception e) {
            log.error("IP: {} -> Data conversion error for password. " + req.body(), req.ip());
        }
        return null;
    }

    private EmailDTO toEmailModel(Request req) {
        try {
            return Global.gson.fromJson(req.body(), EmailDTO.class);
        } catch (Exception e) {
            log.error("IP: {} -> Data conversion error for email. " + req.body(), req.ip());
        }
        return null;
    }

    private LoginDTO toLoginModel(Request req) {
        try {
            return Global.gson.fromJson(req.body(), LoginDTO.class);
        } catch (Exception e) {
            log.error("IP: {} -> Data conversion error for login. " + req.body(), req.ip());
        }
        return null;
    }

}
