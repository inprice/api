package io.inprice.scrapper.api.rest.controller;

import static spark.Spark.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.AuthService;
import spark.Request;

public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final AuthService service = Beans.getSingleton(AuthService.class);

    @Routing
    public void routes() {

        post(Consts.Paths.Auth.LOGIN, (req, res) -> {
            return Commons.createResponse(res, service.login(toLoginModel(req)));
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.REFRESH_TOKEN, (req, res) -> {
        	String token = req.body();
        	String ip = req.ip();
        	String userAgent = req.userAgent();
        	ServiceResponse serres = service.refreshTokens(token, ip, userAgent);
        	if (serres.isOK())
        		return Commons.createResponse(res, serres);
        	else
        		return Responses._401;
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.FORGOT_PASSWORD, (req, res) -> {
            return Commons.createResponse(res, service.forgotPassword(toEmailModel(req), req.ip()));
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.RESET_PASSWORD, (req, res) -> {
            return Commons.createResponse(res, service.resetPassword(toPasswordModel(req)));
        }, Global.gson::toJson);

        post(Consts.Paths.Auth.LOGOUT, (req, res) -> {
            return Commons.createResponse(res, service.logout(req));
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
        	LoginDTO loginDTO = Global.gson.fromJson(req.body(), LoginDTO.class);
        	loginDTO.setIp(req.ip());
        	loginDTO.setUserAgent(req.userAgent());
        	return loginDTO;
        } catch (Exception e) {
            log.error("IP: {} -> Data conversion error for login. " + req.body(), req.ip());
        }
        return null;
    }

}
