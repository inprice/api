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
    private final Set<String> workspaceNeededURIs;

    private final Set<String> sensitiveMethodsSet;

    public AuthFilter() {
        allowedURIs = new HashSet<>(5);
        allowedURIs.add(Consts.Paths.Company.REGISTER);
        allowedURIs.add(Consts.Paths.Auth.LOGIN);
        allowedURIs.add(Consts.Paths.Auth.FORGOT_PASSWORD);
        allowedURIs.add(Consts.Paths.Auth.RESET_PASSWORD);
        allowedURIs.add(Consts.Paths.Auth.LOGOUT);

        workspaceNeededURIs = new HashSet<>(2);
        workspaceNeededURIs.add(Consts.Paths.Product.BASE);
        workspaceNeededURIs.add(Consts.Paths.Link.BASE);

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
                        halt(HttpStatus.FORBIDDEN_403, "Unauthorized user!");
                    }

                    Context.setAuthUser(authUser);

                    if (isWorkspaceNeeded(request)) {
                        boolean isWorkspaceSet = false;
                        String workspace = request.headers(Consts.Auth.WORKSPACE_HEADER);
                        if (workspace != null) {
                            try {
                                Long wsId = Long.valueOf(workspace);
                                Context.setWorkspaceId(wsId);
                                isWorkspaceSet = true;
                            } catch (Exception e) {
                                //
                            }
                        }
                        if (! isWorkspaceSet) {
                            halt(HttpStatus.NOT_ACCEPTABLE_406, "Workspace is missing!");
                        }
                    }

                }
            }
        }
    }

    private boolean isAuthenticationNeeded(Request req) {
        return ! (allowedURIs.contains(req.uri()));
    }

    private boolean isWorkspaceNeeded(Request req) {
        if (workspaceNeededURIs.contains(req.uri())) return true;
        for (String uri: workspaceNeededURIs) {
            if (req.uri().startsWith(uri)) return true;
        }
        return false;
    }

}
