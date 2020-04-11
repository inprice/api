package io.inprice.scrapper.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.bitwalker.useragentutils.UserAgent;
import io.inprice.scrapper.api.app.member.Member;
import io.inprice.scrapper.api.app.member.MemberRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
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
                  if (checkCompany(user)) {
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

   private boolean checkCompany(User user) {
      if (user.getLastCompanyId() != null) {
         ServiceResponse found = memberRepository.findByEmailAndCompanyId(user.getEmail(), user.getLastCompanyId());
         if (found.isOK()) {
            Member member = found.getData();
            user.setRole(member.getRole());
            user.setCompanyName(member.getCompanyName());
            return true;
         }
      }

      ServiceResponse found = memberRepository.findASuitableCompanyId(user.getEmail());
      if (found.isOK()) {
         Member member = found.getData();
         ServiceResponse updated = userRepository.updateLastCompany(user.getId(), member.getCompanyId());
         if (updated.isOK()) {
            user.setRole(member.getRole());
            user.setLastCompanyId(member.getCompanyId());
            user.setCompanyName(member.getCompanyName());
            return true;
         }
      }

      return false;
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
      List<String> tokens = ctx.cookieStore(Consts.Cookie.SESSIONS);
      if (tokens != null && tokens.size() > 0) {
         boolean result = authRepository.deleteByTokenHashes(tokens);
         ctx.clearCookieStore();
         if (result) return Responses.OK;
      }
      return Responses.Already.LOGGED_OUT;
   }

   public ServiceResponse createSession(Context ctx, User user) {
      AuthUser authUser = new AuthUser();
      authUser.setUserId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setUserName(user.getName());
      authUser.setCompanyId(user.getLastCompanyId());
      authUser.setCompanyName(user.getCompanyName());
      authUser.setRole(user.getRole().name());

      String token = SessionHelper.toToken(authUser);
      UserAgent ua = UserAgent.parseUserAgentString(ctx.userAgent());

      UserSession session = new UserSession();
      session.setTokenHash(DigestUtils.md5Hex(token));
      session.setUserId(user.getId());
      session.setCompanyId(user.getLastCompanyId());
      session.setIp(ctx.ip());
      session.setOs(ua.getOperatingSystem().getName());
      session.setBrowser(ua.getBrowser().getName());
      session.setUserAgent(ctx.userAgent());

      ServiceResponse res = authRepository.saveSession(session);
      if (res.isOK()) {
         List<String> tokens = ctx.cookieStore(Consts.Cookie.SESSIONS);
         if (tokens == null) tokens = new ArrayList<>(1);
         tokens.add(token);
         ctx.cookieStore(Consts.Cookie.SESSIONS, tokens);
      }
      return res;
   }

}
