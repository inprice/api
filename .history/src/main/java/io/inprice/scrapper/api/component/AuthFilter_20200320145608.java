package io.inprice.scrapper.api.component;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.AuthUser;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AuthFilter implements Handler {

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

      sensitiveMethodsSet = new HashSet<>(3);
      sensitiveMethodsSet.add("DELETE");
      sensitiveMethodsSet.add("PUT");
      sensitiveMethodsSet.add("POST");

      log.info("Allowed URIs");
      for (String uri : allowedURIs) {
         log.info(" - " + uri);
      }
   }

   @Override
   public void handle(Context ctx) throws Exception {
      if ("options".equals(ctx.method().toLowerCase())) {
         ctx.status(HttpStatus.OK_200);
         return;
      }

      if (isAuthenticationNeeded(ctx.contextPath())) {
         String authToken = ctx.header(Consts.Auth.AUTHORIZATION_TOKEN);
         if (authHeader == null) {
            ctx.status(HttpStatus.UNAUTHORIZED_401);
         } else {
            if (tokenService.isTokenInvalidated(authHeader)) {
               ctx.status(HttpStatus.UNAUTHORIZED_401);
            } else {
               if (isRefreshTokenRequest(ctx.contextPath())) {

                  final String token = ctx.body();
                  if (tokenService.isTokenInvalidated(token)) {
                     ctx.status(HttpStatus.UNAUTHORIZED_401);
                  } else {
                     boolean expired = tokenService.isRefreshTokenExpiredOrSuspicious(token, ctx.ip(), ctx.userAgent());
                     if (expired) {
                        ctx.status(HttpStatus.UNAUTHORIZED_401);
                     }
                  }
               } else {
                  AuthUser authUser = tokenService.checkAccessToken(token);
                  if (authUser == null) {
                     ctx.status(HttpStatus.UNAUTHORIZED_401);
                  } else if (!Role.ADMIN.equals(authUser.getRole())
                        && ctx.contextPath().startsWith(Consts.Paths.ADMIN_BASE)) {
                     ctx.res.setStatus(HttpStatus.FORBIDDEN_403);
                  } else if (Role.USER.equals(authUser.getRole()) && sensitiveMethodsSet.contains(ctx.method())) {
                     // readers are allowed to update their passwords
                     if (!ctx.contextPath().equals(Consts.Paths.User.PASSWORD)) {
                        ctx.status(HttpStatus.FORBIDDEN_403);
                     }
                  }
                  // everything is OK!
                  if (ctx.status() < 400) {
                     UserInfo.setAuthUser(authUser);
                  }
               }
            }
         }
      }
      if (ctx.status() >= 400) {
         throw new Exception("Unauthorized.");
      }
   }

   private boolean isRefreshTokenRequest(String uri) {
      return uri.startsWith("/refresh-token");
   }

   private boolean isAuthenticationNeeded(String uri) {
      return !(allowedURIs.contains(uri));
   }

}
