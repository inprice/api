package io.inprice.scrapper.api.session;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.HandlerInterruptException;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.info.AuthUser;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AuthFilter implements Handler {

   private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

   private static final TokenService tokenService = Beans.getSingleton(TokenService.class);

   private final Set<String> allowedURIs;
   private final Set<String> sensitiveMethodsSet;

   public AuthFilter() {
      allowedURIs = new HashSet<>(5);
      allowedURIs.add(Consts.Paths.Auth.REGISTER_REQUEST);
      allowedURIs.add(Consts.Paths.Auth.REGISTER);
      allowedURIs.add(Consts.Paths.Auth.INVITATION);

      allowedURIs.add(Consts.Paths.Auth.LOGIN);
      allowedURIs.add(Consts.Paths.Auth.LOGOUT);
      allowedURIs.add(Consts.Paths.Auth.FORGOT_PASSWORD);
      allowedURIs.add(Consts.Paths.Auth.RESET_PASSWORD);

      if (Props.isRunningForDev()) {
         allowedURIs.add("/routes");
      }

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
      String URI = ctx.req.getRequestURI().toLowerCase();
      if (URI.charAt(URI.length()-1) == '/') URI = URI.substring(0, URI.length()-1);

      if (isAuthenticationNeeded(URI)) {
         String accessToken = ctx.header(Consts.Auth.AUTHORIZATION_HEADER);
         if (accessToken == null) {
            ctx.status(HttpStatus.UNAUTHORIZED_401);
         } else {
            if (tokenService.isTokenInvalidated(accessToken)) {
               ctx.status(HttpStatus.UNAUTHORIZED_401);
            } else {

               if (isRefreshTokenRequest(URI)) {
                  final String token = ctx.body();
                  if (tokenService.isTokenInvalidated(token)) {
                     ctx.status(HttpStatus.UNAUTHORIZED_401);
                  } else {
                     boolean expired = tokenService.isRefreshTokenExpired(token, ctx.ip(), ctx.userAgent());
                     if (expired) {
                        ctx.status(HttpStatus.UNAUTHORIZED_401);
                     }
                  }
               } else {
                  AuthUser authUser = tokenService.checkAccessToken(accessToken);
                  if (authUser == null) {
                     ctx.status(HttpStatus.UNAUTHORIZED_401);
                  } else if (!MemberRole.ADMIN.equals(authUser.getRole())
                        && URI.startsWith(Consts.Paths.ADMIN_BASE)) {
                           ctx.status(HttpStatus.FORBIDDEN_403);
                  } else if (MemberRole.READER.equals(authUser.getRole()) && sensitiveMethodsSet.contains(ctx.method())) {
                     // readers are allowed to update their passwords
                     if (!URI.equals(Consts.Paths.User.PASSWORD)) {
                        ctx.status(HttpStatus.FORBIDDEN_403);
                     }
                  }
                  //everything is ok
                  CurrentUser.setAuthUser(authUser);
               }
            }
         }
      }
      if (ctx.status() >= 400) {
         String reason = "Bad request";
         if (ctx.status() == HttpStatus.UNAUTHORIZED_401) reason = "Unauthorized";
         if (ctx.status() == HttpStatus.FORBIDDEN_403) reason = "Forbidden";
         throw new HandlerInterruptException(ctx.status(), reason);
      }
   }

   private boolean isRefreshTokenRequest(String uri) {
      return uri.startsWith(Consts.Paths.Auth.REFRESH_TOKEN);
   }

   private boolean isAuthenticationNeeded(String uri) {
      return !(allowedURIs.contains(uri));
   }

}
