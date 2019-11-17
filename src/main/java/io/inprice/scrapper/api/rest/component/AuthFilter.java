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
    private final Set<String> sensitiveMethodsSet;

    public AuthFilter() {
        allowedURIs = new HashSet<>(5);
        allowedURIs.add(Consts.Paths.Auth.REGISTER);
        allowedURIs.add(Consts.Paths.Auth.LOGIN);
        allowedURIs.add(Consts.Paths.Auth.FORGOT_PASSWORD);
        allowedURIs.add(Consts.Paths.Auth.RESET_PASSWORD);

        sensitiveMethodsSet = new HashSet<>(2);
        sensitiveMethodsSet.add("DELETE");
        sensitiveMethodsSet.add("PUT");
        sensitiveMethodsSet.add("POST");

        log.info("Allowed URIs");
        for (String uri: allowedURIs) {
            log.info(" - " + uri);
        }
    }

    public void handle(Request request, Response response) {
        if ("options".equals(request.requestMethod().toLowerCase())) return;

        if (isAuthenticationNeeded(request)) {
            String authHeader = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
            if (authHeader == null) {
                halt(HttpStatus.UNAUTHORIZED_401, "Missing authentication header!");
            } else {
                String token = tokenService.getToken(request);
                if (tokenService.isTokenInvalidated(token)) {
                    halt(HttpStatus.NOT_ACCEPTABLE_406, "Invalidated token!");
                } else {
                    AuthUser authUser = tokenService.isTokenExpired(token);
                    if (authUser == null) {
                        halt(HttpStatus.REQUEST_TIMEOUT_408, "Expired token!");
                    } else if (! UserType.ADMIN.equals(authUser.getType()) && request.uri().startsWith(Consts.Paths.ADMIN_BASE)) {
                        halt(HttpStatus.FORBIDDEN_403, "Unauthorized user!");
                    } else if (UserType.READER.equals(authUser.getType()) && sensitiveMethodsSet.contains(request.requestMethod())) {
                        //readers are allowed to update their passwords
                        if (! request.uri().equals(Consts.Paths.User.PASSWORD)) {
                            halt(HttpStatus.FORBIDDEN_403, "Unauthorized user!");
                        }
                    }
                    //everything is ok!
                    Context.setAuthUser(authUser);
                }
            }
        }
    }

    private boolean isAuthenticationNeeded(Request req) {
        return ! (allowedURIs.contains(req.uri()));
    }

}
