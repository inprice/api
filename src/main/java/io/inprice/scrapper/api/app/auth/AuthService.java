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
import io.inprice.scrapper.api.app.membership.MembershipRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
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
import io.inprice.scrapper.api.helpers.ClientSide;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.api.helpers.SessionHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
import io.inprice.scrapper.api.session.info.ForCookie;
import io.inprice.scrapper.api.session.info.ForDatabase;
import io.inprice.scrapper.api.session.info.ForRedis;
import io.inprice.scrapper.api.session.info.ForResponse;
import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.meta.AppEnv;
import io.inprice.scrapper.common.models.Membership;
import io.inprice.scrapper.common.models.User;
import io.javalin.http.Context;
import jodd.util.BCrypt;

public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
  private final MembershipRepository membershipRepository = Beans.getSingleton(MembershipRepository.class);
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
    if (!res.isOK())
      return res;

    String problem = EmailValidator.verify(email);
    if (problem == null) {

      ServiceResponse found = userRepository.findByEmail(email);
      if (found.isOK()) {

        User user = found.getData();
        try {
          Map<String, Object> dataMap = new HashMap<>(3);
          dataMap.put("user", user.getName());
          dataMap.put("token", TokenService.add(TokenType.FORGOT_PASSWORD, email));
          dataMap.put("url", Props.APP_WEB_URL() + Consts.Paths.Auth.RESET_PASSWORD);

          final String message = renderer.renderForgotPassword(dataMap);
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
            authRepository.closeByUserId(user.getId());
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

      ControllerHelper.removeExpiredAuthCookie(ctx);

      String tokenString = ctx.cookie(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> cookieSesList = SessionHelper.fromToken(tokenString);
        if (cookieSesList != null && cookieSesList.size() > 0) {
          authRepository.closeSession(cookieSesList);
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
              Map<String, Object> data = new HashMap<>(2);
              data.put("sessionNo", sessionNo);
              data.put("sessions", responseSesList);
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

    ServiceResponse res = membershipRepository.getUserCompanies(user.getEmail());
    if (res.isOK()) {

      List<Membership> membershipList = res.getData();

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

        ForResponse responseSes = new ForResponse(cookieSes, user.getName(), mem.getCompanyName(), user.getTimezone(),
            mem.getCurrencyFormat());
        responseSesList.add(responseSes);

        ForRedis redisSes = new ForRedis(responseSes, user.getId(), mem.getCompanyId(), cookieSes.getHash());
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
        res = authRepository.saveSessions(redisSesList, dbSesList);

        if (res.isOK()) {
          String tokenString = SessionHelper.toToken(cookieSesList);
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
