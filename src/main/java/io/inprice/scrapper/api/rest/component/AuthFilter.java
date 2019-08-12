package io.inprice.scrapper.api.rest.component;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.rest.service.AuthService;
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

    private final AuthService authService = Beans.getSingleton(AuthService.class);
    private final Set<String> allowedURIs;

    public AuthFilter() {
        allowedURIs = new HashSet<>(3);
        allowedURIs.add(Consts.Paths.Intro.LOGIN);
        allowedURIs.add(Consts.Paths.Intro.REFRESH_TOKEN);
        allowedURIs.add(Consts.Paths.Intro.FORGOT_PASSWORD);
        allowedURIs.add(Consts.Paths.Intro.RESET_PASSWORD);
        allowedURIs.add(Consts.Paths.Intro.LOGOUT);
        allowedURIs.add(Consts.Paths.Company.REGISTER);

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
                String token = authHeader.replace(Consts.Auth.TOKEN_PREFIX, "");
                if (! authService.validateToken(token)) {
                    log.warn("Expired token!");
                    halt(HttpStatus.UNAUTHORIZED_401);
                }
            }
        }
    }

    private boolean isAuthenticationNeeded(Request req) {
        final String uri = req.uri();

        //todo: should be check the trailing part of each uri
        if (allowedURIs.contains(uri)) return false;

        for (String u: allowedURIs) {
            if (uri.startsWith(u)) return false;
        }
        return true;
    }

}
