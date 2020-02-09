package io.inprice.scrapper.api.rest.component;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.rest.service.TokenService;
import io.inprice.scrapper.common.meta.Role;
import spark.Filter;
import spark.Request;
import spark.Response;

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
		for (String uri : allowedURIs) {
			log.info(" - " + uri);
		}
	}

	public void handle(Request request, Response response) {
		if ("options".equals(request.requestMethod().toLowerCase())) {
			response.status(HttpStatus.OK_200);
			return;
		} 

		if (isRefreshTokenRequest(request.uri())) {
			final String token = request.body();
			if (tokenService.isTokenInvalidated(token)) {
				response.status(HttpStatus.UNAUTHORIZED_401);
			} else {
				boolean expired = tokenService.isRefreshTokenExpiredOrSuspicious(token, request.ip(),
						request.userAgent());
				if (expired) {
					response.status(HttpStatus.UNAUTHORIZED_401);
				}
			}
		} else if (isAuthenticationNeeded(request)) {
			String authHeader = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
			if (authHeader == null) {
				response.status(HttpStatus.UNAUTHORIZED_401);
			} else {
				String token = tokenService.getToken(request);
				if (tokenService.isTokenInvalidated(token)) {
					response.status(HttpStatus.UNAUTHORIZED_401);
				} else {
					AuthUser authUser = tokenService.isAccessTokenExpired(token);
					if (authUser == null) {
						response.status(HttpStatus.UNAUTHORIZED_401);
					} else if (!Role.admin.equals(authUser.getRole())
							&& request.uri().startsWith(Consts.Paths.ADMIN_BASE)) {
						response.status(HttpStatus.FORBIDDEN_403);
					} else if (Role.reader.equals(authUser.getRole())
							&& sensitiveMethodsSet.contains(request.requestMethod())) {
						// readers are allowed to update their passwords
						if (!request.uri().equals(Consts.Paths.User.PASSWORD)) {
							response.status(HttpStatus.FORBIDDEN_403);
						}
					}
					// everything is OK!
					if (response.status() < 400) Context.setAuthUser(authUser);
				}
			}
		}
	}

	private boolean isRefreshTokenRequest(String uri) {
		return uri.startsWith("/refresh-token");
	}

	private boolean isAuthenticationNeeded(Request req) {
		return !(allowedURIs.contains(req.uri()));
	}

}
