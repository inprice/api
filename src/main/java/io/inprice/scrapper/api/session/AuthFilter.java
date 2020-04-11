package io.inprice.scrapper.api.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.SessionHelper;
import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.framework.HandlerInterruptException;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.utils.NumberUtils;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AuthFilter implements Handler {

   private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

   private final Set<String> allowedURIs;
   private final Set<String> sensitiveMethodsSet;

   public AuthFilter() {
      allowedURIs = new HashSet<>(7);
      allowedURIs.add(Consts.Paths.Auth.REQUEST_REGISTRATION);
      allowedURIs.add(Consts.Paths.Auth.COMPLETE_REGISTRATION);
      allowedURIs.add(Consts.Paths.Invitation.ACCEPT_NEW);

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
      if ("OPTIONS".equals(ctx.method())) return;

      String URI = ctx.req.getRequestURI().toLowerCase();
      if (URI.charAt(URI.length()-1) == '/') URI = URI.substring(0, URI.length()-1);

      if (isAuthenticationNeeded(URI)) {
         String token = extractToken(ctx);
         if (token == null) {
            ctx.status(HttpStatus.UNAUTHORIZED_401);
         } else {
            AuthUser authUser = SessionHelper.fromToken(token);
            if (authUser == null) {
               ctx.status(HttpStatus.UNAUTHORIZED_401);
            } else {
               if (!MemberRole.ADMIN.name().equals(authUser.getRole()) && URI.startsWith(Consts.Paths.ADMIN_BASE)) {
                  ctx.status(HttpStatus.FORBIDDEN_403);
               } else
               // viewers should be able to update their passwords
               if (MemberRole.VIEWER.name().equals(authUser.getRole()) && sensitiveMethodsSet.contains(ctx.method())) {
                  if (URI.indexOf(Consts.Paths.User.BASE + "/") < 0) {
                     ctx.status(HttpStatus.FORBIDDEN_403);
                  }
               }
            }
            if (ctx.status() < 400) CurrentUser.setAuthUser(authUser);
         }
      }

      if (ctx.status() >= 400) {
         String reason = "Bad request";
         if (ctx.status() == HttpStatus.UNAUTHORIZED_401) reason = "Unauthorized";
         if (ctx.status() == HttpStatus.FORBIDDEN_403) reason = "Forbidden";
         throw new HandlerInterruptException(ctx.status(), reason);
      }
   }

   private boolean isAuthenticationNeeded(String uri) {
      return !(allowedURIs.contains(uri));
   }

   private String extractToken(Context ctx) {
      String token = null;
      try {
         int sesNo = NumberUtils.toInteger(ctx.header(Consts.SESSION_NO));
         if (sesNo > -1) {
            List<String> tokens = ctx.cookieStore(Consts.Cookie.SESSIONS);
            if (tokens != null && tokens.size() > sesNo) {
               return tokens.get(sesNo);
            }
         }
      } catch (Exception ignored) {}
      return token;
   }

}
