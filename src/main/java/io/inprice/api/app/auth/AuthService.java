package io.inprice.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.bitwalker.useragentutils.UserAgent;
import io.inprice.api.app.auth.dto.InvitationAcceptDTO;
import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.member.MemberDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.dto.LoginDTO;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.app.user.validator.PasswordValidator;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.CookieHelper;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.info.Response;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.session.info.ForRedis;
import io.inprice.api.session.info.ForResponse;
import io.inprice.api.token.TokenType;
import io.inprice.api.token.Tokens;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Member;
import io.inprice.common.models.User;
import io.javalin.http.Context;

public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer templateRenderer = Beans.getSingleton(TemplateRenderer.class);

  Response login(Context ctx, LoginDTO dto) {
    if (dto != null) {
      String problem = verifyLogin(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);

          User user = userDao.findByEmailWithPassword(dto.getEmail());
          if (user != null) {
          	if (!user.isBanned()) {

              if (PasswordHelper.isValid(dto.getPassword(), user.getPassword())) {
              	user.setPassword(null);

              	if (user.isPrivileged()) { //if a super user!
              		ctx.cookie(CookieHelper.createSuperCookie(SessionHelper.toTokenForSuper(user)));

                  List<ForResponse> sesList = new ArrayList<>(1);
                  sesList.add(
                		new ForResponse(
              				null,
              				user.getName(),
              				user.getEmail(),
              				user.getPassword()
          					)
              		);

                  Map<String, Object> sesInfoMap = new HashMap<>(3);
                  sesInfoMap.put("sessionNo", 0);
                  sesInfoMap.put("sessions", sesList);
                  sesInfoMap.put("isPriviledge", Boolean.TRUE);
              		return new Response(sesInfoMap);
              	} else {
                  Map<String, Object> sesInfo = findSessionInfoByEmail(ctx, user.getEmail());
                  if (sesInfo != null && sesInfo.size() > 0) {
                    return new Response(sesInfo);
                  } else {
                    return createSession(ctx, user);
                  }
              	}
              }
            } else {
              return Responses.BANNED_USER;
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
    Response res = Responses.OK;
    if (SysProps.APP_ENV().equals(AppEnv.PROD)) {
      res = RedisClient.isEmailRequested(RateLimiterType.FORGOT_PASSWORD, email);
    }
    if (!res.isOK()) return res;

    String problem = EmailValidator.verify(email);
    if (problem == null) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);
  
        User user = userDao.findByEmail(email);
        if (user != null) {
        	if (! user.isBanned()) {
            try {
              Map<String, Object> dataMap = new HashMap<>(3);
              dataMap.put("user", user.getName());
              dataMap.put("token", Tokens.add(TokenType.FORGOT_PASSWORD, email));
              dataMap.put("url", Props.APP_WEB_URL() + Consts.Paths.Auth.RESET_PASSWORD);

              final String message = templateRenderer.render(EmailTemplate.FORGOT_PASSWORD, dataMap);
              emailSender.send(Props.APP_EMAIL_SENDER(), "Reset your password for inprice.io", user.getEmail(), message);

              return Responses.OK;
            } catch (Exception e) {
              log.error("Failed to render email for forgetting password", e);
              return Responses.ServerProblem.EXCEPTION;
            }
          } else {
            return Responses.BANNED_USER;
          }
        }
      }
    }
    return Responses.NotFound.EMAIL;
  }

  Response resetPassword(Context ctx, PasswordDTO dto) {
    if (dto != null) {
      String problem = PasswordValidator.verify(dto, true, false);

      if (problem == null) {
        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);
          UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);

          final String email = Tokens.get(TokenType.FORGOT_PASSWORD, dto.getToken());
          if (email != null) {

            User user = userDao.findByEmail(email);
            if (user != null) {

              String saltedHash = PasswordHelper.getSaltedHash(dto.getPassword());
              boolean isOK = userDao.updatePassword(user.getId(), saltedHash);

              //closing session
              if (isOK) {
                Tokens.remove(TokenType.FORGOT_PASSWORD, dto.getToken());
                List<ForDatabase> sessions = userSessionDao.findListByUserId(user.getId());
                if (sessions != null && sessions.size() > 0) {
                  for (ForDatabase ses : sessions) {
                    RedisClient.removeSesion(ses.getHash());
                  }
                  userSessionDao.deleteByUserId(user.getId());
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
    if (ctx.cookieMap().containsKey(Consts.SUPER_SESSION)) {
      CookieHelper.removeSuperCookie(ctx);
    }

  	if (ctx.cookieMap().containsKey(Consts.SESSION)) {
      CookieHelper.removeUserCookie(ctx);

      String tokenString = ctx.cookie(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> sessions = SessionHelper.fromTokenForUser(tokenString);
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
              UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
              isOK = userSessionDao.deleteByHashList(hashList);
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
      UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
      MemberDao memberDao = handle.attach(MemberDao.class);

      List<Member> memberList = memberDao.findListByEmailAndStatus(user.getEmail(), UserStatus.JOINED.name());
      if (memberList != null && memberList.size() > 0) {

        List<ForRedis> redisSesList = new ArrayList<>();
        List<ForCookie> sessions = null;
        List<ForDatabase> dbSesList = new ArrayList<>();
        List<ForResponse> responseSesList = new ArrayList<>();

        if (ctx.cookieMap().containsKey(Consts.SESSION)) {
          String tokenString = ctx.cookie(Consts.SESSION);
          if (StringUtils.isNotBlank(tokenString)) {
            sessions = SessionHelper.fromTokenForUser(tokenString);
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

        for (Member mem : memberList) {
          ForCookie cookieSes = new ForCookie(user.getEmail(), mem.getRole().name());
          sessions.add(cookieSes);

          ForResponse responseSes = new ForResponse(cookieSes, user, mem);
          responseSesList.add(responseSes);

          ForRedis redisSes = new ForRedis(responseSes, mem, cookieSes.getHash());
          redisSesList.add(redisSes);

          ForDatabase dbSes = new ForDatabase();
          dbSes.setHash(cookieSes.getHash());
          dbSes.setUserId(mem.getUserId());
          dbSes.setAccountId(mem.getAccountId());
          dbSes.setIp(ipAddress);
          dbSes.setOs(ua.getOperatingSystem().getName());
          dbSes.setBrowser(ua.getBrowser().getName());
          dbSes.setUserAgent(ctx.userAgent());
          dbSesList.add(dbSes);
        }

        if (dbSesList.size() > 0) {
          if (RedisClient.addSesions(redisSesList)) {

            userSessionDao.insertBulk(dbSesList);
            ctx.cookie(CookieHelper.createUserCookie(SessionHelper.toTokenForUser(sessions)));

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

          User user = userDao.findByEmail(sendDto.getEmail());
          if (user == null || !user.isBanned()) {

          	MemberDao memberDao = handle.attach(MemberDao.class);
          	Member member = memberDao.findByEmailAndStatus(sendDto.getEmail(), UserStatus.PENDING.name(), sendDto.getAccountId());
            if (member != null) {

              if (user == null) { //user creation
                UserDTO dto = new UserDTO();
                dto.setName(sendDto.getEmail().split("@")[0]);
                dto.setEmail(sendDto.getEmail());
                dto.setTimezone(timezone);

                String saltedHash = PasswordHelper.getSaltedHash(dto.getPassword());
                long savedId = userDao.insert(dto.getEmail(), saltedHash, dto.getName(), dto.getTimezone());

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
                  memberDao.activate(
                    newUser.getId(),
                    UserStatus.PENDING,
                    UserStatus.JOINED,
                    newUser.getEmail(),
                    sendDto.getAccountId()
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
          } else {
            res = Responses.BANNED_USER;
          }
        }
      } else {
        res = Responses.Invalid.TOKEN;
      }

      return res;
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

        List<ForCookie> sessions = SessionHelper.fromTokenForUser(tokenString);
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

  public static String verifyLogin(LoginDTO dto) {
    String problem = PasswordValidator.verify(dto, false, false);
    if (problem == null) {
      problem = EmailValidator.verify(dto.getEmail());
    }
    return problem;
  }

}
