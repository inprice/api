package io.inprice.scrapper.api.session;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.SessionHelper;
import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.app.user.Membership;
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

         Long userId = NumberUtils.toLong(ctx.header(Consts.USER_ID));
         Long companyId = NumberUtils.toLong(ctx.header(Consts.COMPANY_ID));

         ctx.status(HttpStatus.UNAUTHORIZED_401);

         if (userId != null && userId > 0 && companyId != null && companyId > 0) {

            String token = ctx.cookie(Consts.Cookie.SESSION + userId);
            if (token != null) {

               AuthUser authUser = SessionHelper.fromToken(token);
               if (authUser != null) {

                  Membership membership = authUser.getMemberships().get(companyId);
                  if (membership != null) {

                     if (URI.startsWith(Consts.Paths.ADMIN_BASE)
                     && !MemberRole.ADMIN.equals(membership.getRole())) {
                        ctx.status(HttpStatus.FORBIDDEN_403);

                     } else 
                     if (MemberRole.VIEWER.equals(membership.getRole())
                     && sensitiveMethodsSet.contains(ctx.method())
                     && URI.indexOf(Consts.Paths.User.BASE + "/") < 0) {
                        ctx.status(HttpStatus.FORBIDDEN_403);

                     } else {
                        ctx.status(HttpStatus.OK_200);
                        CurrentUser.set(authUser, companyId);
                     }
                  }
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

   private boolean isAuthenticationNeeded(String uri) {
      return !(allowedURIs.contains(uri));
   }

}
