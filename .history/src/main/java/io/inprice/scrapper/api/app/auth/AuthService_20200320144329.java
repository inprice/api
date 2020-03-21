package io.inprice.scrapper.api.app.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.member.MemberRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.LoginDTOValidator;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordDTOValidator;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Props;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import jodd.util.BCrypt;

public class AuthService {

   private static final Logger log = LoggerFactory.getLogger(AuthService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);
   private final TokenService tokenService = Beans.getSingleton(TokenService.class);

   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

   public ServiceResponse login(LoginDTO loginDTO) {
      if (loginDTO != null) {
         ServiceResponse res = validate(loginDTO);
         if (res.isOK()) {
            ServiceResponse findingRes = userRepository.findByEmail(loginDTO.getEmail(), true);
            if (findingRes.isOK()) {
               User user = findingRes.getData();
               String salt = user.getPasswordSalt();
               String hash = BCrypt.hashpw(loginDTO.getPassword(), salt);
               if (hash.equals(user.getPasswordHash())) {
                  if (checkCompany(user)) {
                     user.setPasswordSalt(null);
                     user.setPasswordHash(null);
                     return authenticatedResponse(user, loginDTO.getIp(), loginDTO.getUserAgent());
                  } else {
                     return Responses.PermissionProblem.NO_COMPANY;
                  }
               }
            }
         }
      }
      return Responses.Invalid.EMAIL_OR_PASSWORD;
   }

   private boolean checkCompany(User user) {
      if (user.getCompanyId() == null) {
         ServiceResponse found = memberRepository.findASuitableCompanyId(user.getEmail());
         if (found.isOK()) {
            ServiceResponse updated = userRepository.setCompanyId(user.getId(), found.getData());
            if (updated.isOK()) {
               user.setCompanyId(found.getData());
               return true;
            }
         }
      }
      return false;
   }

   public ServiceResponse forgotPassword(EmailDTO emailDTO, String ip) {
      if (emailDTO != null) {
         if (RedisClient.hasAlreadyRequestedForgotPassById(ip)) {
            return Responses.Illegal.TOO_MUCH_REQUEST;
         }
         RedisClient.addRequesterIpForForgotPass(ip);

         ServiceResponse res = validateEmail(emailDTO);
         if (res.isOK()) {

            ServiceResponse found = userRepository.findByEmail(emailDTO.getEmail());
            if (found.isOK()) {

               User user = found.getData();
               final String token = tokenService.getForgotPasswordToken(emailDTO.getEmail());

               try {
                  Map<String, Object> dataMap = new HashMap<>(3);
                  dataMap.put("name", user.getName());
                  dataMap.put("token", token);
                  dataMap.put("baseUrl", Props.getFrontendBaseUrl());

                  final String message = renderer.renderForgotPassword(dataMap);
                  emailSender.send(Props.getEmail_Sender(), "Reset your password", user.getEmail(), message);
               } catch (Exception e) {
                  log.error("An error occurred in rendering email for forgetting password", e);
                  return Responses.ServerProblem.EXCEPTION;
               }
            }
         }
         return res;
      }
      return Responses.Invalid.EMAIL;
   }

   public ServiceResponse resetPassword(PasswordDTO passwordDTO) {
      if (passwordDTO != null) {
         ServiceResponse res = validatePassword(passwordDTO);
         if (res.isOK()) {
            if (!tokenService.isTokenInvalidated(passwordDTO.getToken())) {
               final String email = tokenService.extractEmail(passwordDTO.getToken());

               if (email != null) {
                  ServiceResponse found = userRepository.findByEmail(email);
                  if (found.isOK()) {

                     User user = found.getData();
                     AuthUser authUser = new AuthUser();
                     authUser.setId(user.getId());
                     authUser.setCompanyId(user.getCompanyId());
                     authUser.setName(user.getName());
                     res = userRepository.updatePassword(passwordDTO, authUser);
                     tokenService.revokeToken(TokenType.PASSWORD_RESET, passwordDTO.getToken());
                  } else {
                     return Responses.NotFound.EMAIL;
                  }
               } else {
                  return Responses.Illegal.TIMED_OUT_FORGOT_PASSWORD;
               }
            } else {
               return Responses.Invalid.TOKEN;
            }
         }
         return res;
      }

      return Responses.Invalid.PASSWORD;
   }

   public ServiceResponse refreshTokens(String token, String ip, String userAgent) {
      if (StringUtils.isNotBlank(token) && !tokenService.isTokenInvalidated(token)) {
         tokenService.revokeToken(TokenType.REFRESH, token);
         String bareRefreshToken = tokenService.extractRefreshToken(token);
         if (bareRefreshToken != null) {
            String[] tokenParts = bareRefreshToken.split("::");
            ServiceResponse found = userRepository.findByEmail(tokenParts[0]);
            if (found.isOK()) {
               return authenticatedResponse(found.getData(), ip, userAgent);
            }
         }
      }
      return Responses.Invalid.TOKEN;
   }

   public ServiceResponse logout(String authHeader) {
      tokenService.revokeToken(tokenService.getToken(authHeader));
      return Responses.OK;
   }

   private ServiceResponse validateEmail(EmailDTO emailDTO) {
      String result = EmailValidator.verify(emailDTO.getEmail();
      if (result != null)
         return new ServiceResponse(result);
      else
         return Responses.OK;
   }

   private ServiceResponse validatePassword(PasswordDTO passwordDTO) {
      List<String> problems = PasswordDTOValidator.verify(passwordDTO, true, false);

      if (StringUtils.isBlank(passwordDTO.getToken())) {
         problems.add("Token cannot be null!");
      } else if (tokenService.isEmailTokenExpired(passwordDTO.getToken())) {
         problems.add("Your token has expired!");
      }

      return Commons.createResponse(problems);
   }

   private ServiceResponse validate(LoginDTO loginDTO) {
      List<String> problems = LoginDTOValidator.verify(loginDTO);
      return Commons.createResponse(problems);
   }

   Tokens createTokens(User user, String ip, String userToken) {
      AuthUser authUser = new AuthUser();
      authUser.setId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setFullName(user.getFullName());
      authUser.setRole(user.getRole());
      authUser.setCompanyId(user.getCompanyId());
      authUser.setWorkspaceId(user.getWorkspaceId());

      return createTokens(authUser, ip, userToken);
   }

   Tokens createTokens(AuthUser user, String ip, String userToken) {
      AuthUser authUser = new AuthUser();
      authUser.setId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setFullName(user.getFullName());
      authUser.setRole(user.getRole());
      authUser.setCompanyId(user.getCompanyId());
      authUser.setWorkspaceId(user.getWorkspaceId());

      return tokenService.generateTokens(authUser, ip, userToken);
   }

   private ServiceResponse authenticatedResponse(User user, String ip, String userAgent) {
      Tokens tokens = createTokens(user, ip, userAgent);
      Map<String, Object> payload = new HashMap<>(2);
      payload.put("user", user);
      payload.put("tokens", tokens);
      return new ServiceResponse(payload);
   }

}
