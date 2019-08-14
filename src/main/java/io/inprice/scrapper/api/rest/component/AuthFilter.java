package io.inprice.scrapper.api.rest.component;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.rest.service.TokenService;
import io.inprice.scrapper.common.meta.UserType;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.HashSet;
import java.util.Set;

import static spark.Spark.halt;

public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private static final TokenService tokenService = Beans.getSingleton(TokenService.class);
    private final Set<String> allowedURIs;

    public AuthFilter() {
        allowedURIs = new HashSet<>(5);
        allowedURIs.add(Consts.Paths.Company.REGISTER);
        allowedURIs.add(Consts.Paths.Auth.LOGIN);
        allowedURIs.add(Consts.Paths.Auth.FORGOT_PASSWORD);
        allowedURIs.add(Consts.Paths.Auth.RESET_PASSWORD);
        allowedURIs.add(Consts.Paths.Auth.LOGOUT);

        log.info("Allowed URIs");
        for (String uri: allowedURIs) {
            log.info(" - " + uri);
        }
    }

    public void handle(Request request, Response response) {
        if (isAuthenticationNeeded(request)) {
            String authHeader = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
            if (authHeader == null) {
                log.warn("Missing header: " + Consts.Auth.AUTHORIZATION_HEADER);
                halt(HttpStatus.UNAUTHORIZED_401);
            } else {
                String token = tokenService.getToken(request);
                if (tokenService.isTokenInvalidated(token)) {
                    log.warn("Invalidated token!");
                    halt(HttpStatus.NOT_ACCEPTABLE_406);
                } else {
                    AuthUser authUser = tokenService.isTokenExpired(token);
                    if (authUser == null) {
                        log.warn("Expired token!");
                        halt(HttpStatus.REQUEST_TIMEOUT_408);
                    } else if (! UserType.ADMIN.equals(authUser.getType()) && request.uri().startsWith(Consts.Paths.ADMIN_BASE)) {
                        log.warn("Unauthorized user!");
                        halt(HttpStatus.UNAUTHORIZED_401);
                    }
                }
            }
        }
    }

    private boolean isAuthenticationNeeded(Request req) {
        final String uri = req.uri();
        if (allowedURIs.contains(uri)) return false;
        for (String u: allowedURIs) {
            if (uri.startsWith(u)) return false;
        }
        return true;
    }

}