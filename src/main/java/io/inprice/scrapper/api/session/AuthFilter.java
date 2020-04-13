package io.inprice.scrapper.api.session;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.AuthRepository;
import io.inprice.scrapper.api.app.auth.AuthUser;
import io.inprice.scrapper.api.app.auth.SessionHelper;
import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.app.user.UserCompany;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.HandlerInterruptException;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.utils.NumberUtils;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AuthFilter implements Handler {

   private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
   private final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);

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

         ctx.status(HttpStatus.UNAUTHORIZED_401);

         Long userId = NumberUtils.toLong(ctx.header(Consts.USER_ID), 0L);
         Integer companyNo = NumberUtils.toInteger(ctx.header(Consts.COMPANY_NO), -1);

         if (userId > 0 && companyNo > -1) {

            String token = ctx.cookie(Consts.Cookie.SESSION + userId);
            if (token != null) {

               AuthUser authUser = SessionHelper.fromToken(token);
               if (authUser != null) {

                  UserCompany uc = authUser.getCompanies().get(companyNo);
                  if (uc != null) {

                     ServiceResponse res = authRepository.findByToken(uc.getToken());
                     if (res.isOK()) {

                        if (URI.startsWith(Consts.Paths.ADMIN_BASE)
                        && !MemberRole.ADMIN.equals(uc.getRole())) {
                           ctx.status(HttpStatus.FORBIDDEN_403);

                        } else 
                        if (MemberRole.VIEWER.equals(uc.getRole())
                        && sensitiveMethodsSet.contains(ctx.method())
                        && URI.indexOf(Consts.Paths.User.BASE + "/") < 0) {
                           ctx.status(HttpStatus.FORBIDDEN_403);

                        } else {
                           CurrentUser.set(authUser, uc);
                        }
                     }
                  }
               }
            }
            if (ctx.status() >= 400) {
               ctx.removeCookie(Consts.Cookie.SESSION + userId);
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
