package io.inprice.scrapper.api.app.auth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.inprice.scrapper.api.info.SessionTokens;
import io.inprice.scrapper.api.meta.RateLimiterType;
import jodd.util.BCrypt;

public class AuthService {

   private static final Logger log = LoggerFactory.getLogger(AuthService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);
   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

   public ServiceResponse login(LoginDTO dto) {
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
                     return createTokens(user);
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

   public ServiceResponse resetPassword(PasswordDTO dto) {
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
                  return createTokens(user);
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

   public ServiceResponse refreshTokens(String refreshToken) {
      AuthUser found = TokenService.get(TokenType.REFRESH, refreshToken);
      if (found != null) {
         return createTokens(found);
      } else if (refreshToken != null) {
         TokenService.remove(TokenType.REFRESH, refreshToken);
         return Responses._401;
      }
      return Responses.Invalid.TOKEN;
   }

   public ServiceResponse logout(String email) {
      String problem = EmailValidator.verify(email);
      if (problem == null) {
         closeSession(email);
         return Responses.OK;
      }
      return Responses.Already.LOGGED_OUT;
   }

   public ServiceResponse createTokens(User user) {
      AuthUser authUser = new AuthUser();
      authUser.setId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setName(user.getName());
      authUser.setRole(user.getRole());
      authUser.setCompanyId(user.getLastCompanyId());

      return createTokens(authUser);
   }

   ServiceResponse createTokens(AuthUser user) {
      // old ones must be remevoed first, if any
      closeSession(user.getEmail());

      // creates new tokens
      String access = TokenService.add(TokenType.ACCESS, user);
      String refresh = TokenService.add(TokenType.REFRESH, user);

      // creates session info
      SessionTokens tokens = new SessionTokens(access, refresh);
      TokenService.addSessionTokens(user.getEmail(), tokens);

      // establishes returning object
      Map<String, Serializable> data = new HashMap<>(3);
      data.put(TokenType.ACCESS.name(), tokens.getAccess());
      data.put(TokenType.REFRESH.name(), tokens.getRefresh());
      data.put("user", user);

      log.info(user.getEmail() + " has just logged in.");

      return new ServiceResponse(data);
   }

   public void closeSession(String email) {
      SessionTokens tokens = TokenService.getSessionTokens(email);
      if (tokens != null) {
         TokenService.remove(TokenType.ACCESS, tokens.getAccess());
         TokenService.remove(TokenType.REFRESH, tokens.getRefresh());
         log.info(email + " has just logged out.");
      }
      TokenService.removeSessionTokens(email);
   }

}
