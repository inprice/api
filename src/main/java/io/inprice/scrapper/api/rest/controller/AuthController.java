package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AuthService;
import io.inprice.scrapper.api.rest.service.CompanyService;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import static spark.Spark.post;

public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final CompanyService companyService = Beans.getSingleton(CompanyService.class);
    private final AuthService authService = Beans.getSingleton(AuthService.class);

    public void init() {

        post(Consts.Auth.AUTH_ENDPOINT_PREFIX + "/register", (req, res) -> {
            ServiceResponse serviceRes = register(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Auth.AUTH_ENDPOINT_PREFIX + "/login", (req, res) -> {
            ServiceResponse serviceRes = login(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Auth.AUTH_ENDPOINT_PREFIX + "/refresh-token", (req, res) -> {
            ServiceResponse serviceRes = refresh(req, res);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(Consts.Auth.AUTH_ENDPOINT_PREFIX + "/logout", (req, res) -> logout(req));

    }

    private ServiceResponse register(String body) {
        CompanyDTO companyDTO = toCompanyModel(body);
        if (companyDTO != null) {
            return companyService.insert(companyDTO);
        }
        log.error("Invalid sign up data: " + body);
        return new ServiceResponse(HttpStatus.BAD_REQUEST_400, "Invalid data for sign up!");
    }

    private ServiceResponse login(Request req, Response res) {
        LoginDTO loginDTO = toLoginModel(req.body());
        if (loginDTO != null) {
            ServiceResponse<AuthUser> serviceRes = authService.login(loginDTO);
            if (serviceRes.isOK()) {
                res.header(Consts.Auth.AUTHORIZATION_HEADER, Consts.Auth.TOKEN_PREFIX + " " + authService.newToken(serviceRes.getModel()));
            }
            return InstantResponses.OK;
        }
        return InstantResponses.INVALID_PARAM("Email or password");
    }

    private ServiceResponse refresh(Request req, Response res) {
        final String token = getToken(req);
        authService.revokeToken(token);

        AuthUser authUser = authService.getAuthenticatedUser(token);
        res.header(Consts.Auth.AUTHORIZATION_HEADER, Consts.Auth.TOKEN_PREFIX + " " + authService.newToken(authUser));

        return InstantResponses.OK;
    }

    private ServiceResponse logout(Request req) {
        authService.revokeToken(getToken(req));
        return InstantResponses.OK;
    }

    private CompanyDTO toCompanyModel(String body) {
        try {
            return Global.gson.fromJson(body, CompanyDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for company, body: " + body);
        }

        return null;
    }

    private String getToken(Request request) {
        String header = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
        if (header != null && header.length() > 0)
            return header.replace(Consts.Auth.TOKEN_PREFIX, "");
        else
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
