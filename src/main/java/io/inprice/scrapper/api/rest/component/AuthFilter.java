package io.inprice.scrapper.api.rest.component;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.rest.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;

import static spark.Spark.halt;

public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final TokenService tokenService = Beans.getSingleton(TokenService.class);

    public void handle(Request request, Response response) {
        if (!isLoginRequest(request) && !isRegistrationRequest(request)) {
            String authHeader = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
            if (authHeader == null) {
                log.warn("Missing header: Authorization");
                halt(401);
            } else if (!tokenService.validateToken(authHeader.replace(Consts.Auth.TOKEN_PREFIX, ""))) {
                log.warn("Expired token :" + authHeader);
                halt(401);
            }
        }
    }

    private boolean isLoginRequest(Request request) {
        return request.uri().equals(Consts.Auth.AUTH_ENDPOINT_PREFIX + Consts.Auth.LOGIN_ENDPOINT);
    }

    private boolean isRegistrationRequest(Request request) {
        return request.uri().equals(Consts.Auth.AUTH_ENDPOINT_PREFIX + Consts.Auth.REGISTRATION_ENDPOINT);
    }

}
