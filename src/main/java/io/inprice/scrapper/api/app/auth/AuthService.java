package io.inprice.scrapper.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.bitwalker.useragentutils.UserAgent;
import io.inprice.scrapper.api.app.member.MemberRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.Membership;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.LoginDTOValidator;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
import io.javalin.http.Context;
import jodd.util.BCrypt;

public class AuthService {

   private static final Logger log = LoggerFactory.getLogger(AuthService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);
   private final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);
   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

   public ServiceResponse login(Context ctx, LoginDTO dto) {
      if (dto != null) {
         String problem = LoginDTOValidator.verify(dto);
         if (problem == null) {

            ServiceResponse found = userRepository.findByEmail(dto.getEmail(), true);
            if (found.isOK()) {
               User user = found.getData();
               String salt = user.getPasswordSalt();
               String hash = BCrypt.hashpw(dto.getPassword(), salt);
               if (hash.equals(user.getPasswordHash())) {
                  found = memberRepository.getMembershipByEmail(user.getEmail());
                  if (found.isOK()) {
                     user.setMemberships(found.getData());
                     return createSession(ctx, user);
                  } else {
                     return Responses.PermissionProblem.NO_COMPANY;
                  }
               }
            }
         } else {
            return new ServiceResponse(problem);
         }
      }
      return Responses.Invalid.EMAIL_OR_PASSWORD;
   }

   public ServiceResponse forgotPassword(String email, String ip) {
      ServiceResponse res = RedisClient.isIpRateLimited(RateLimiterType.FORGOT_PASSWORD, ip);
      if (! res.isOK()) return res;

      String problem = EmailValidator.verify(email);
      if (problem == null) {

         ServiceResponse found = userRepository.findByEmail(email);
         if (found.isOK()) {

            User user = found.getData();
            try {
               Map<String, Object> dataMap = new HashMap<>(3);
               dataMap.put("user", user.getName());
               dataMap.put("token", TokenService.add(TokenType.FORGOT_PASSWORD, email));
               dataMap.put("url", Props.getWebUrl() + Consts.Paths.Auth.RESET_PASSWORD);

               final String message = renderer.renderForgotPassword(dataMap);
               emailSender.send(Props.getEmail_Sender(), "Reset your password", user.getEmail(), message);

               return Responses.OK;
            } catch (Exception e) {
               log.error("An error occurred in rendering email for forgetting password", e);
               return Responses.ServerProblem.EXCEPTION;
            }
         } else {
            return Responses.NotFound.EMAIL;
         }
      }
      return Responses.Invalid.EMAIL;
   }

   public ServiceResponse resetPassword(Context ctx, PasswordDTO dto) {
      if (dto != null) {

         String problem = PasswordValidator.verify(dto, true, false);
         if (problem == null) {
            
            final String email = TokenService.get(TokenType.FORGOT_PASSWORD, dto.getToken());
            ServiceResponse found = userRepository.findByEmail(email);
            
            if (found.isOK()) {
               User user = found.getData();
               ServiceResponse res = userRepository.updatePassword(user.getId(), dto.getPassword());
               if (res.isOK()) {
                  TokenService.remove(TokenType.FORGOT_PASSWORD, dto.getToken());
                  authRepository.deleteByUserId(user.getId());
                  return createSession(ctx, user);
               }
            } else {
               TokenService.remove(TokenType.FORGOT_PASSWORD, dto.getToken());
               return Responses.NotFound.EMAIL;
            }
         } else {
            return new ServiceResponse(problem);
         }
      }
      return Responses.Invalid.DATA;
   }

   public ServiceResponse logout(Context ctx) {
      boolean result = false;
      for (Entry<String, String> entry : ctx.cookieMap().entrySet()) {
         if (entry.getKey().startsWith(Consts.Cookie.SESSION)) {

            AuthUser authUser = SessionHelper.fromToken(entry.getValue());
            if (authUser != null) {
               result = true;
               authRepository.deleteSession(authUser);
               ctx.removeCookie(entry.getKey());
               log.info("Logout {}", authUser.toString());
            }
         }
      }
      if (result) {
         return Responses.OK;
      } else {
         return Responses.Already.LOGGED_OUT;
      }
   }

   public ServiceResponse createSession(Context ctx, User user) {
      AuthUser authUser = new AuthUser();
      authUser.setUserId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setUserName(user.getName());
      for (Membership ms : user.getMemberships()) {
         authUser.getMemberships().put(ms.getCompanyId(), ms);
      }

      String token = SessionHelper.toToken(authUser);
      UserAgent ua = UserAgent.parseUserAgentString(ctx.userAgent());

      List<UserSession> sessions = new ArrayList<>();
      for (Membership ms : user.getMemberships()) {
         UserSession uses = new UserSession();
         uses.setToken(ms.getToken());
         uses.setUserId(user.getId());
         uses.setCompanyId(ms.getCompanyId());
         uses.setIp(ctx.ip());
         uses.setOs(ua.getOperatingSystem().getName());
         uses.setBrowser(ua.getBrowser().getName());
         uses.setUserAgent(ctx.userAgent());
         sessions.add(uses);
      }
      if (sessions.size() > 0) {
         ServiceResponse res = authRepository.saveSessions(sessions);
         if (res.isOK()) {
            ctx.cookie(Consts.Cookie.SESSION + user.getId(), token);
         }
         return res;
      }
      return Responses.PermissionProblem.UNAUTHORIZED;
   }

}
