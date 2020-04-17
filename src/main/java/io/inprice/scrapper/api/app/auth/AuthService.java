package io.inprice.scrapper.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.bitwalker.useragentutils.UserAgent;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user_company.UserCompany;
import io.inprice.scrapper.api.app.user_company.UserCompanyRepository;
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
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
import io.javalin.http.Context;
import jodd.util.BCrypt;

public class AuthService {

   private static final Logger log = LoggerFactory.getLogger(AuthService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final UserCompanyRepository userCompanyRepository = Beans.getSingleton(UserCompanyRepository.class);
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
                  Integer oldSessionNo = findSessionNo(ctx, user.getEmail());
                  if (oldSessionNo != null) {
                     return new ServiceResponse(oldSessionNo);
                  } else {
                     return createSession(ctx, user);
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
      if (ctx.cookieMap().containsKey(Consts.SESSION)) {
         String token = ctx.cookie(Consts.SESSION);
         ctx.removeCookie(Consts.SESSION);
         if (StringUtils.isNotBlank(token)) {

            List<SessionInfoForToken> sessionTokens = SessionHelper.fromToken(token);
            if (sessionTokens != null && sessionTokens.size() > 0) {
               authRepository.deleteSession(sessionTokens);
               return Responses.OK;
            }
         }
      }
      return Responses.Already.LOGGED_OUT;
   }

   public Integer findSessionNo(Context ctx, String email) {
      Integer sessionNo = null;
      if (ctx.cookieMap().containsKey(Consts.SESSION)) {
         String token = ctx.cookie(Consts.SESSION);
         if (StringUtils.isNotBlank(token)) {
            List<SessionInfoForToken> sessionTokens = SessionHelper.fromToken(token);
            if (sessionTokens != null && sessionTokens.size() > 0) {
               for (int i=0; i<sessionTokens.size(); i++) {
                  SessionInfoForToken sestok = sessionTokens.get(i);
                  if (sestok.getEmail().equals(email)) {
                     sessionNo = i;
                     break;
                  }
               }
            }
         }
      }
      return sessionNo;
   }

   public ServiceResponse createSession(Context ctx, User user) {
      Integer sessionNo = null;

      ServiceResponse res = userCompanyRepository.getUserCompanies(user.getEmail());
      if (res.isOK()) {

         List<UserCompany> userCompanies = res.getData();
         List<SessionInfoForDB> sessionsForDB = new ArrayList<>();
         List<SessionInfoForToken> sessionsForToken = null;

         if (ctx.cookieMap().containsKey(Consts.SESSION)) {
            String token = ctx.cookie(Consts.SESSION);
            if (StringUtils.isNotBlank(token)) {
               sessionsForToken = SessionHelper.fromToken(token);
            }
         }
         if (sessionsForToken == null) sessionsForToken = new ArrayList<>();
         sessionNo = sessionsForToken.size();

         UserAgent ua = new UserAgent(ctx.userAgent());
         for (UserCompany uc: userCompanies) {
            SessionInfoForDB ses = new SessionInfoForDB();
            ses.setHash(UUID.randomUUID().toString().replaceAll("-", "").toUpperCase());
            ses.setUserId(uc.getUserId());
            ses.setCompanyId(uc.getCompanyId());
            ses.setIp(ctx.ip());
            ses.setOs(ua.getOperatingSystem().getName());
            ses.setBrowser(ua.getBrowser().getName());
            ses.setUserAgent(ctx.userAgent());
            sessionsForDB.add(ses);

            sessionsForToken.add(
               new SessionInfoForToken(
                  user.getName(),
                  user.getEmail(),
                  uc.getCompanyName(),
                  uc.getRole()
               )
            );
         }

         if (sessionsForDB.size() > 0) {
            res = authRepository.saveSessions(sessionsForDB);

            if (res.isOK()) {
               String token = SessionHelper.toToken(sessionsForToken);
               Cookie serverCookie = new Cookie(Consts.SESSION, token);
               serverCookie.setHttpOnly(true);
               serverCookie.setSecure(! Props.isRunningForDev());
               ctx.cookie(serverCookie);

               // response
               Map<String, Object> data = new HashMap<>(2);
               data.put("sessionNo", sessionNo);
               data.put("sessions", sessionsForToken);
               res = new ServiceResponse(data);
            }
         } else {
            res = Responses.ServerProblem.EXCEPTION;
         }
      }
      return res;
   }

}
