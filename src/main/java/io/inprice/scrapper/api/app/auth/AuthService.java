package io.inprice.scrapper.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import io.inprice.scrapper.api.helpers.SessionHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
import io.inprice.scrapper.api.session.info.ForCookie;
import io.inprice.scrapper.api.session.info.ForDatabase;
import io.inprice.scrapper.api.session.info.ForRedis;
import io.inprice.scrapper.api.session.info.ForResponse;
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

                  found = findSessionNo(ctx, user.getEmail());
                  if (found.isOK()) {
                     return found;
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
         String tokenString = ctx.cookie(Consts.SESSION);
         ctx.removeCookie(Consts.SESSION);
         if (StringUtils.isNotBlank(tokenString)) {

            List<ForCookie> cookieSesList = SessionHelper.fromToken(tokenString);
            if (cookieSesList != null && cookieSesList.size() > 0) {
               authRepository.deleteSession(cookieSesList);
               return Responses.OK;
            }
         }
      }
      return Responses.Already.LOGGED_OUT;
   }

   public ServiceResponse findSessionNo(Context ctx, String email) {
      if (ctx.cookieMap().containsKey(Consts.SESSION)) {

         String tokenString = ctx.cookie(Consts.SESSION);
         if (StringUtils.isNotBlank(tokenString)) {

            List<ForCookie> cookieSesList = SessionHelper.fromToken(tokenString);
            if (cookieSesList != null && cookieSesList.size() > 0) {

               for (int i=0; i<cookieSesList.size(); i++) {
                  ForCookie token = cookieSesList.get(i);
                  if (token.getEmail().equals(email)) {
                     Map<String, Object> data = new HashMap<>(2);
                     data.put("sessionNo", i);
                     data.put("sessions", cookieSesList);
                     return new ServiceResponse(data);
                  }
               }
            }
         }
      }
      return Responses.NotFound.DATA;
   }

   public ServiceResponse createSession(Context ctx, User user) {
      Integer sessionNo = null;

      ServiceResponse res = userCompanyRepository.getUserCompanies(user.getEmail());
      if (res.isOK()) {

         List<UserCompany> userCompanyList = res.getData();

         List<ForCookie> cookieSesList = null;
         List<ForRedis> redisSesList = new ArrayList<>();
         List<ForDatabase> dbSesList = new ArrayList<>();
         List<ForResponse> responseSesList = new ArrayList<>();

         if (ctx.cookieMap().containsKey(Consts.SESSION)) {
            String tokenString = ctx.cookie(Consts.SESSION);
            if (StringUtils.isNotBlank(tokenString)) {
               cookieSesList = SessionHelper.fromToken(tokenString);
            }
         }
         if (cookieSesList == null) {
            cookieSesList = new ArrayList<>();
         } else {
            for (ForCookie cookieSes: cookieSesList) {
               ForRedis redisSes = RedisClient.getSession(cookieSes.getHash());
               if (redisSes != null) {
                  responseSesList.add(new ForResponse(cookieSes, redisSes.getUser(), redisSes.getCompany()));
               }
            }
         }
         sessionNo = cookieSesList.size();

         UserAgent ua = new UserAgent(ctx.userAgent());
         for (UserCompany uc: userCompanyList) {

            ForCookie cookieSes = new ForCookie(user.getEmail(), uc.getRole());
            cookieSesList.add(cookieSes);

            ForResponse responseSes = new ForResponse(
               cookieSes,
               user.getName(),
               uc.getCompanyName()
            );
            responseSesList.add(responseSes);

            ForRedis redisSes = new ForRedis(
               responseSes,
               user.getId(),
               uc.getCompanyId(),
               cookieSes.getHash()
            );
            redisSesList.add(redisSes);

            ForDatabase dbSes = new ForDatabase();
            dbSes.setHash(cookieSes.getHash());
            dbSes.setUserId(uc.getUserId());
            dbSes.setCompanyId(uc.getCompanyId());
            dbSes.setIp(ctx.ip());
            dbSes.setOs(ua.getOperatingSystem().getName());
            dbSes.setBrowser(ua.getBrowser().getName());
            dbSes.setUserAgent(ctx.userAgent());
            dbSesList.add(dbSes);
         }

         if (dbSesList.size() > 0) {
            res = authRepository.saveSessions(redisSesList, dbSesList);

            if (res.isOK()) {
               String tokenString = SessionHelper.toToken(cookieSesList);
               Cookie cookie = new Cookie(Consts.SESSION, tokenString);
               cookie.setHttpOnly(true);
               if (! Props.isRunningForDev()) {
                  cookie.setDomain(".inprice.io");
                  cookie.setMaxAge(Integer.MAX_VALUE);
                  cookie.setSecure(true);
               }
               ctx.cookie(cookie);

               // response
               Map<String, Object> data = new HashMap<>(2);
               data.put("sessionNo", sessionNo);
               data.put("sessions", responseSesList);
               res = new ServiceResponse(data);
            }
         } else {
            res = Responses.ServerProblem.EXCEPTION;
         }
      }
      return res;
   }

}
