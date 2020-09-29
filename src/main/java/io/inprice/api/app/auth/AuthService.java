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
import io.inprice.api.app.auth.dto.InvitationAcceptDTO;
import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.auth.dto.LoginDTO;
import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.auth.dto.UserDTO;
import io.inprice.api.app.membership.MembershipDao;
import io.inprice.api.app.token.Tokens;
import io.inprice.api.app.user.UserDao;
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
import io.inprice.api.info.Response;
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
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
import io.javalin.http.Context;
import jodd.util.BCrypt;

public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer templateRenderer = Beans.getSingleton(TemplateRenderer.class);

  Response login(Context ctx, LoginDTO dto) {
    if (dto != null) {
      String problem = AuthValidator.verify(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);
    
          User user = userDao.findByEmail(dto.getEmail());
          if (user != null) {
            String salt = user.getPasswordSalt();
            String hash = BCrypt.hashpw(dto.getPassword(), salt);

            if (hash.equals(user.getPasswordHash())) {
              Map<String, Object> sessionInfo = findSessionInfoByEmail(ctx, user.getEmail());
              if (sessionInfo != null && sessionInfo.size() > 0) {
                return new Response(sessionInfo);
              } else {
                return createSession(ctx, user);
              }
            }
          }
        }
      } else {
        return new Response(problem);
      }
    }
    return Responses.Invalid.EMAIL_OR_PASSWORD;
  }

  Response forgotPassword(String email) {
    Response res = RedisClient.isEmailRequested(RateLimiterType.FORGOT_PASSWORD, email);
    if (!res.isOK()) return res;

    String problem = EmailValidator.verify(email);
    if (problem == null) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);
  
        User user = userDao.findByEmail(email);
        if (user != null) {
          try {
            Map<String, Object> dataMap = new HashMap<>(3);
            dataMap.put("user", user.getName());
            dataMap.put("token", Tokens.add(TokenType.FORGOT_PASSWORD, email));
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
    }
    return Responses.Invalid.EMAIL;
  }

  Response resetPassword(Context ctx, PasswordDTO dto) {
    if (dto != null) {
      String problem = PasswordValidator.verify(dto, true, false);

      if (problem == null) {
        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);
          SessionDao sessionDao = handle.attach(SessionDao.class);

          final String email = Tokens.get(TokenType.FORGOT_PASSWORD, dto.getToken());
          if (email != null) {

            User user = userDao.findByEmail(email);
            if (user != null) {
              final String salt = BCrypt.gensalt(Props.APP_SALT_ROUNDS());
              final String hash = BCrypt.hashpw(dto.getPassword(), salt);
              boolean isOK = userDao.updatePassword(user.getId(), salt, hash);

              //closing session
              if (isOK) {
                Tokens.remove(TokenType.FORGOT_PASSWORD, dto.getToken());
                List<ForDatabase> sessions = sessionDao.findListByUserId(user.getId());
                if (sessions != null && sessions.size() > 0) {
                  for (ForDatabase ses : sessions) {
                    RedisClient.removeSesion(ses.getHash());
                  }
                  sessionDao.deleteByUserId(user.getId());
                }
                return createSession(ctx, user);

              } else {
                Tokens.remove(TokenType.FORGOT_PASSWORD, dto.getToken());
                return Responses.NotFound.EMAIL;
              }
            }
          }
        }
      } else {
        return new Response(problem);
      }
    }
    return Responses.Invalid.DATA;
  }

  Response logout(Context ctx) {
    if (ctx.cookieMap().containsKey(Consts.SESSION)) {
      CookieHelper.removeAuthCookie(ctx);

      String tokenString = ctx.cookie(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> sessions = SessionHelper.fromToken(tokenString);
        if (sessions != null && sessions.size() > 0) {
          
          List<String> hashList = new ArrayList<>(sessions.size());
          for (ForCookie ses : sessions) {
            RedisClient.removeSesion(ses.getHash());
            hashList.add(ses.getHash());
            log.info("Logout {}", ses.toString());
          }
      
          boolean isOK = false;
          if (hashList.size() > 0) {
            try (Handle handle = Database.getHandle()) {
              SessionDao sessionDao = handle.attach(SessionDao.class);
              isOK = sessionDao.deleteByHashList(hashList);
            }
          }

          if (isOK) return Responses.OK;
        }
      }
    }
    return Responses.Already.LOGGED_OUT;
  }

  public Response createSession(Context ctx, User user) {
    Integer sessionNo = null;

    try (Handle handle = Database.getHandle()) {
      SessionDao sessionDao = handle.attach(SessionDao.class);
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      List<Membership> membershipList = membershipDao.findListByEmailAndStatus(user.getEmail(), UserStatus.JOINED.name());
      if (membershipList != null && membershipList.size() > 0) {

        List<ForRedis> redisSesList = new ArrayList<>();
        List<ForCookie> sessions = null;
        List<ForDatabase> dbSesList = new ArrayList<>();
        List<ForResponse> responseSesList = new ArrayList<>();

        if (ctx.cookieMap().containsKey(Consts.SESSION)) {
          String tokenString = ctx.cookie(Consts.SESSION);
          if (StringUtils.isNotBlank(tokenString)) {
            sessions = SessionHelper.fromToken(tokenString);
          }
        }
        if (sessions == null) {
          sessions = new ArrayList<>();
        } else {
          for (ForCookie cookieSes : sessions) {
            ForRedis redisSes = RedisClient.getSession(cookieSes.getHash());
            if (redisSes != null) {
              responseSesList.add(new ForResponse(cookieSes, redisSes));
            }
          }
        }
        sessionNo = sessions.size();

        String ipAddress = ClientSide.getIp(ctx.req);
        UserAgent ua = new UserAgent(ctx.userAgent());

        for (Membership mem : membershipList) {
          ForCookie cookieSes = new ForCookie(user.getEmail(), mem.getRole().name());
          sessions.add(cookieSes);

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
          boolean isSaved = false;

          if (RedisClient.addSesions(redisSesList)) {
            boolean[] anyAffected = sessionDao.insert(dbSesList);
            if (anyAffected != null && anyAffected.length > 0) {
              for (boolean b : anyAffected) {
                if (b) {
                  isSaved = true;
                  break;
                }
              }
            }
          }

          if (isSaved) {
            String tokenString = SessionHelper.toToken(sessions);

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
            return new Response(map);
          }
        }
        return Responses.DataProblem.DB_PROBLEM;
      }
    }
    return Responses.NotFound.MEMBERSHIP;
  }

  Response acceptNewUser(InvitationAcceptDTO acceptDto, String timezone) {
    String problem = validateInvitation(acceptDto);

    if (problem == null) {
      Response res = Responses.DataProblem.DB_PROBLEM;

      InvitationSendDTO sendDto = Tokens.get(TokenType.INVITATION, acceptDto.getToken());
      if (sendDto != null) {

        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);
          MembershipDao membershipDao = handle.attach(MembershipDao.class);
    
          Membership membership = membershipDao.findListByEmailAndStatusAndCompanyId(sendDto.getEmail(), UserStatus.PENDING.name(), sendDto.getCompanyId());
          if (membership != null) {

            User user = userDao.findByEmail(sendDto.getEmail());
            if (user == null) { //user creation
              UserDTO dto = new UserDTO();
              dto.setName(sendDto.getEmail().split("@")[0]);
              dto.setEmail(sendDto.getEmail());
              dto.setTimezone(timezone);

              final String salt = BCrypt.gensalt(Props.APP_SALT_ROUNDS());
              final String hash = BCrypt.hashpw(acceptDto.getPassword(), salt);

              long savedId = userDao.insert(dto.getEmail(), dto.getName(), dto.getTimezone(), salt, hash);

              if (savedId > 0) {
                User newUser = new User(); //user in response is needed for auto login
                newUser.setId(savedId);
                newUser.setEmail(dto.getEmail());
                newUser.setName(dto.getName());
                newUser.setTimezone(dto.getTimezone());
                res = new Response(user);
              }
            } else {
              res = Responses.Already.Defined.MEMBERSHIP;
            }

            if (res.isOK()) {
              User newUser = res.getData();
              boolean isActivated = 
                membershipDao.activate(
                  newUser.getId(),
                  UserStatus.JOINED.name(),
                  newUser.getEmail(),
                  UserStatus.PENDING.name(),
                  sendDto.getCompanyId()
                );

              if (isActivated) {
                Tokens.remove(TokenType.INVITATION, acceptDto.getToken());
              } else {
                res = Responses.NotFound.MEMBERSHIP;
              }
            }
    
          } else {
            res = Responses.NotActive.INVITATION;
          }
        }

      } else {
        return Responses.Invalid.TOKEN;
      }
    }

    return new Response(problem);
  }
              
  private String validateInvitation(InvitationAcceptDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid invitation data!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getToken())) {
        problem = Responses.Invalid.TOKEN.getReason();
      }
    }

    if (problem == null) {
      PasswordDTO pswDTO = new PasswordDTO();
      pswDTO.setPassword(dto.getPassword());
      pswDTO.setRepeatPassword(dto.getRepeatPassword());
      problem = PasswordValidator.verify(pswDTO, true, false);
    }

    return problem;
  }

  private Map<String, Object> findSessionInfoByEmail(Context ctx, String email) {
    if (ctx.cookieMap().containsKey(Consts.SESSION)) {

      String tokenString = ctx.cookie(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> sessions = SessionHelper.fromToken(tokenString);
        if (sessions != null && sessions.size() > 0) {

          Integer sessionNo = null;
          for (int i = 0; i < sessions.size(); i++) {
            ForCookie cookieSes = sessions.get(i);
            if (cookieSes.getEmail().equals(email)) {
              sessionNo = i;
              break;
            }
          }

          if (sessionNo != null && sessionNo > -1) {
            List<ForResponse> responseSesList = new ArrayList<>();

            for (int i = 0; i < sessions.size(); i++) {
              ForCookie cookieSes = sessions.get(i);
              ForRedis redisSes = RedisClient.getSession(cookieSes.getHash());
              if (redisSes != null) {
                responseSesList.add(new ForResponse(cookieSes, redisSes));
              }
            }

            if (responseSesList.size() == sessions.size()) {
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

}
