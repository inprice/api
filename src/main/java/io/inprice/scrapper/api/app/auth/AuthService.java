package io.inprice.scrapper.api.app.auth;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.member.Member;
import io.inprice.scrapper.api.app.member.MemberRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.LoginDTOValidator;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
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

         String problem = LoginDTOValidator.verify(loginDTO);
         if (problem == null) {

            ServiceResponse found = userRepository.findByEmail(loginDTO.getEmail(), true);
            if (found.isOK()) {
               User user = found.getData();
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
      } else {
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
      }
      return false;
   }

   public ServiceResponse forgotPassword(String email, String ip) {
      if (RedisClient.isIpRateLimited(RateLimiterType.FORGOT_PASSWORD, ip)) {
         return Responses.Illegal.TOO_MUCH_REQUEST;
      }
      RedisClient.addIpToRateLimiter(RateLimiterType.FORGOT_PASSWORD, ip);

      String problem = EmailValidator.verify(email);
      if (problem == null) {

         ServiceResponse found = userRepository.findByEmail(email);
         if (found.isOK()) {

            User user = found.getData();
            final String token = tokenService.getResetPasswordToken(email);

            try {
               Map<String, Object> dataMap = new HashMap<>(3);
               dataMap.put("name", user.getName());
               dataMap.put("token", token);
               dataMap.put("url", Props.getBaseUrl() + Consts.Paths.Auth.RESET_PASSWORD);

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
         if (!tokenService.isTokenInvalidated(dto.getToken())) {
   
            ServiceResponse valid = validatePassword(dto);
            if (valid.isOK()) {

               final String email = tokenService.decryptToken(dto.getToken());
               ServiceResponse found = userRepository.findByEmail(email);

               if (found.isOK()) {
                  tokenService.revokeToken(TokenType.PASSWORD_RESET, dto.getToken());
                  User user = found.getData();
                  return userRepository.updatePassword(user.getId(), dto.getPassword());
               } else {
                  return Responses.NotFound.EMAIL;
               }

            } else {
               return valid;
            }
         } else {
            return Responses.Invalid.TOKEN;
         }
      }

      return Responses.Invalid.DATA;
   }

   public ServiceResponse refreshTokens(String refreshToken, String accessToken, String ip, String userAgent) {
      if (StringUtils.isNotBlank(refreshToken) && !tokenService.isTokenInvalidated(refreshToken)) {
         tokenService.revokeToken(TokenType.REFRESH, refreshToken);
         tokenService.revokeToken(TokenType.ACCESS, accessToken);
         String bareRefreshToken = tokenService.decryptToken(refreshToken);
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

   public ServiceResponse logout(String refreshToken, String accessToken) {
      tokenService.revokeToken(TokenType.REFRESH, refreshToken);
      tokenService.revokeToken(TokenType.ACCESS, accessToken);
      return Responses.OK;
   }

   private ServiceResponse validatePassword(PasswordDTO dto) {
      String problem = PasswordValidator.verify(dto, true, false);

      if (problem == null && StringUtils.isBlank(dto.getToken())) {
         problem = "Token cannot be null!";
      }
      
      if (problem == null && tokenService.isTokenExpired(dto.getToken())) {
         problem = "Your token has expired!";
      }

      if (problem == null)
         return Responses.OK;
      else
         return new ServiceResponse(problem);
   }

   private ServiceResponse authenticatedResponse(User user, String ip, String userAgent) {
      Map<TokenType, String> tokens = createTokens(user, ip, userAgent);
      Map<String, Object> payload = new HashMap<>(2);
      payload.put("user", user);
      payload.put("tokens", tokens);
      return new ServiceResponse(payload);
   }

   Map<TokenType, String> createTokens(User user, String ip, String userToken) {
      AuthUser authUser = new AuthUser();
      authUser.setId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setName(user.getName());
      authUser.setRole(user.getRole());
      authUser.setCompanyId(user.getLastCompanyId());

      return createTokens(authUser, ip, userToken);
   }

   Map<TokenType, String> createTokens(AuthUser user, String ip, String userToken) {
      AuthUser authUser = new AuthUser();
      authUser.setId(user.getId());
      authUser.setEmail(user.getEmail());
      authUser.setName(user.getName());
      authUser.setRole(user.getRole());
      authUser.setCompanyId(user.getCompanyId());

      return tokenService.getAccessTokens(authUser, ip, userToken);
   }

}
