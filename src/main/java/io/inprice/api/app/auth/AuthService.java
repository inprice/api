package io.inprice.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.bitwalker.useragentutils.UserAgent;
import io.inprice.api.app.auth.dto.LoginDTO;
import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.token.TokenService;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.CookieHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.session.info.ForRedis;
import io.inprice.api.session.info.ForResponse;
import io.inprice.api.validator.AuthValidator;
import io.inprice.api.validator.EmailValidator;
import io.inprice.api.validator.PasswordValidator;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
import io.javalin.http.Context;
import jodd.util.BCrypt;

public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final AuthRepository repository = Beans.getSingleton(AuthRepository.class);
  private final TemplateRenderer templateRenderer = Beans.getSingleton(TemplateRenderer.class);
  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

  ServiceResponse login(Context ctx, LoginDTO dto) {
    if (dto != null) {
      String problem = AuthValidator.verify(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          AuthDao dao = handle.attach(AuthDao.class);

          User user = dao.findUserByEmail(dto.getEmail());
          if (user != null) {
            String salt = user.getPasswordSalt();
            String hash = BCrypt.hashpw(dto.getPassword(), salt);

            if (hash.equals(user.getPasswordHash())) {
              Map<String, Object> sessionInfo = findSessionInfoByEmail(ctx, user.getEmail());
              if (sessionInfo != null && sessionInfo.size() > 0) {
                return new ServiceResponse(sessionInfo);
              } else {
                return createSession(ctx, user);
              }

            }
          }
        }
      } else {
        return new ServiceResponse(problem);
      }
    }
    return Responses.Invalid.EMAIL_OR_PASSWORD;
  }

  ServiceResponse forgotPassword(String email) {
    ServiceResponse res = RedisClient.isEmailRequested(RateLimiterType.FORGOT_PASSWORD, email);
    if (!res.isOK()) return res;

    String problem = EmailValidator.verify(email);
    if (problem == null) {

      User user = repository.findUserByEmail(email);
      if (user != null) {
        try {
          Map<String, Object> dataMap = new HashMap<>(3);
          dataMap.put("user", user.getName());
          dataMap.put("token", TokenService.add(TokenType.FORGOT_PASSWORD, email));
          dataMap.put("url", Props.APP_WEB_URL() + Consts.Paths.Auth.RESET_PASSWORD);

          final String message = templateRenderer.renderForgotPassword(dataMap);
          emailSender.send(Props.APP_EMAIL_SENDER(), "Reset your password", user.getEmail(), message);

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

  ServiceResponse resetPassword(Context ctx, PasswordDTO dto) {
    if (dto != null) {
      String problem = PasswordValidator.verify(dto, true, false);
      if (problem == null) {
        User user = repository.updateUserPassword(dto);
        if (user != null) {
          TokenService.remove(TokenType.FORGOT_PASSWORD, dto.getToken());
          repository.closeByUserId(user.getId());
          return createSession(ctx, user);
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

  ServiceResponse logout(Context ctx) {
    if (ctx.cookieMap().containsKey(Consts.SESSION)) {

      CookieHelper.removeAuthCookie(ctx);

      String tokenString = ctx.cookie(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> cookieSesList = SessionHelper.fromToken(tokenString);
        if (cookieSesList != null && cookieSesList.size() > 0) {
          boolean res = repository.closeSession(cookieSesList);
          if (res) return Responses.OK;
        }
      }
    }
    return Responses.Already.LOGGED_OUT;
  }

  private Map<String, Object> findSessionInfoByEmail(Context ctx, String email) {
    if (ctx.cookieMap().containsKey(Consts.SESSION)) {

      String tokenString = ctx.cookie(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> cookieSesList = SessionHelper.fromToken(tokenString);
        if (cookieSesList != null && cookieSesList.size() > 0) {

          Integer sessionNo = null;
          for (int i = 0; i < cookieSesList.size(); i++) {
            ForCookie cookieSes = cookieSesList.get(i);
            if (cookieSes.getEmail().equals(email)) {
              sessionNo = i;
              break;
            }
          }

          if (sessionNo != null && sessionNo > -1) {
            List<ForResponse> responseSesList = new ArrayList<>();

            for (int i = 0; i < cookieSesList.size(); i++) {
              ForCookie cookieSes = cookieSesList.get(i);
              ForRedis redisSes = RedisClient.getSession(cookieSes.getHash());
              if (redisSes != null) {
                responseSesList.add(new ForResponse(cookieSes, redisSes));
              }
            }

            if (responseSesList.size() == cookieSesList.size()) {
              Map<String, Object> map = new HashMap<>(2);
              map.put("sessionNo", sessionNo);
              map.put("sessions", responseSesList);
              return map;
            }
          }
        }
      }
    }
    return null;
  }

  public ServiceResponse createSession(Context ctx, User user) {
    Integer sessionNo = null;

    List<Membership> membershipList = repository.getUserMembershipsByEmail(user.getEmail());
    if (membershipList != null && membershipList.size() > 0) {

      List<ForRedis> redisSesList = new ArrayList<>();
      List<ForCookie> cookieSesList = null;
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
        for (ForCookie cookieSes : cookieSesList) {
          ForRedis redisSes = RedisClient.getSession(cookieSes.getHash());
          if (redisSes != null) {
            responseSesList.add(new ForResponse(cookieSes, redisSes));
          }
        }
      }
      sessionNo = cookieSesList.size();

      String ipAddress = ClientSide.getIp(ctx.req);
      UserAgent ua = new UserAgent(ctx.userAgent());

      for (Membership mem : membershipList) {
        ForCookie cookieSes = new ForCookie(user.getEmail(), mem.getRole().name());
        cookieSesList.add(cookieSes);

        ForResponse responseSes = new ForResponse(cookieSes, user, mem);
        responseSesList.add(responseSes);

        ForRedis redisSes = new ForRedis(responseSes, mem, cookieSes.getHash());
        redisSesList.add(redisSes);

        ForDatabase dbSes = new ForDatabase();
        dbSes.setHash(cookieSes.getHash());
        dbSes.setUserId(mem.getUserId());
        dbSes.setCompanyId(mem.getCompanyId());
        dbSes.setIp(ipAddress);
        dbSes.setOs(ua.getOperatingSystem().getName());
        dbSes.setBrowser(ua.getBrowser().getName());
        dbSes.setUserAgent(ctx.userAgent());
        dbSesList.add(dbSes);
      }

      if (dbSesList.size() > 0) {
        boolean saved = repository.saveSessions(redisSesList, dbSesList);

        if (saved) {
          String tokenString = SessionHelper.toToken(cookieSesList);

          /*
           * Please not that: cookies must be deleted in the way they are created below
           * see ControllerHelper class
          */
          Cookie cookie = new Cookie(Consts.SESSION, tokenString);
          cookie.setHttpOnly(true);
          cookie.setSecure(true);
          if (SysProps.APP_ENV().equals(AppEnv.PROD)) {
            cookie.setDomain(".inprice.io");
            cookie.setMaxAge(Integer.MAX_VALUE);
          } else { // for dev and test purposes
            cookie.setMaxAge(60 * 60 * 24); // for one day
          }
          ctx.cookie(cookie);

          // the response
          Map<String, Object> map = new HashMap<>(2);
          map.put("sessionNo", sessionNo);
          map.put("sessions", responseSesList);
          return new ServiceResponse(map);
        } else {
          return Responses.DataProblem.DB_PROBLEM;
        }
      } else {
        return Responses.ServerProblem.EXCEPTION;
      }
    }
    return Responses.NotFound.MEMBERSHIP;
  }

}
