package io.inprice.scrapper.api.app.token;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Cryptor;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.javalin.http.Context;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;

public class TokenService {

   private final TokenRepository tokenRepository = Beans.getSingleton(TokenRepository.class);

   Map<TokenType, String> generateAccessTokens(AuthUser authUser, String ip, String userAgent) {
      String refresh = authUser.getEmail() + "::" + ip + "::" + Consts.Auth.APP_SECRET_KEY + "::" + userAgent;
      String refreshToken = generateToken(TokenType.REFRESH, Cryptor.encrypt(refresh));

      String access = Global.gson.toJson(authUser);
      String accessToken = generateToken(TokenType.ACCESS, Cryptor.encrypt(access));

      Map<TokenType, String> tokensMap = new HashMap<>(2);
      tokensMap.put(TokenType.ACCESS, accessToken);
      tokensMap.put(TokenType.REFRESH, refreshToken);

      return tokensMap;
   }

   Map<TokenType, String> generateInvitationTokens(MemberDTO memberDTO) {
      memberDTO.setTokenType(TokenType.INVITATION_CONFIRM);
      String confirm = Global.gson.toJson(memberDTO);
      String confirmToken = generateToken(TokenType.INVITATION_CONFIRM, Cryptor.encrypt(confirm));

      memberDTO.setTokenType(TokenType.INVITATION_REJECT);
      String reject = Global.gson.toJson(memberDTO);
      String rejectToken = generateToken(TokenType.INVITATION_REJECT, Cryptor.encrypt(reject));

      Map<TokenType, String> tokensMap = new HashMap<>(2);
      tokensMap.put(TokenType.INVITATION_CONFIRM, confirmToken);
      tokensMap.put(TokenType.INVITATION_REJECT, rejectToken);

      return tokensMap;
   }

   String generatePasswordResetToken(String email) {
      return generateToken(TokenType.PASSWORD_RESET, Cryptor.encrypt(email));
   }

   private String generateToken(TokenType tokenType, byte[] payload) {
      Date now = new Date();

      DefaultClaims claims = new DefaultClaims();
      claims.setIssuedAt(now);
      claims.setExpiration(new Date(now.getTime() + tokenType.ttl()));
      claims.put(Consts.Auth.PAYLOAD, payload);

      return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, Consts.Auth.APP_SECRET_KEY).compact();
   }

   public void revokeToken(String token) {
      if (!StringUtils.isBlank(token))
         tokenRepository.invalidateToken(token);
   }

   public AuthUser getAuthUser(Context ctx) {
      String token = getToken(ctx.header(Consts.Auth.AUTHORIZATION_HEADER));
      if (token != null) {
         return getAuthUser(token);
      }
      return null;
   }

   public AuthUser getAuthUser(String token) {
      return Global.gson.fromJson(getTokenString(token), AuthUser.class);
   }

   public String getEmail(String token) {
      return getTokenString(token);
   }

   public String getRefreshString(String token) {
      return getTokenString(token);
   }

   private String getTokenString(String token) {
      try {
         Claims claims = Jwts.parser().setSigningKey(Consts.Auth.APP_SECRET_KEY).parseClaimsJws(token).getBody();

         final byte[] prePayload = Base64.getDecoder().decode(claims.get(Consts.Auth.PAYLOAD, String.class));
         return Cryptor.decrypt(prePayload);
      } catch (ExpiredJwtException ignored) {
         return null;
      }
   }

   public String getToken(String header) {
      if (header != null && header.length() > 0)
         return header.replace(Consts.Auth.TOKEN_PREFIX, "");
      else
         return null;
   }

   public AuthUser isAccessTokenExpired(String token) {
      try {
         return getAuthUser(token);
      } catch (Exception e) {
         return null;
      }
   }

   public boolean isRefreshTokenExpiredOrSuspicious(String token, String ip, String userAgent) {
      try {
         // refresh request must be done from the same ip and device as is it is in the
         // first request
         // so ip and user-agent fields are checked for these controls
         String bareToken = getTokenString(token);
         String[] tokenParts = bareToken.split("::");
         return (!ip.equals(tokenParts[1]) || !userAgent.equals(tokenParts[3]));
      } catch (Exception e) {
         return true;
      }
   }

   boolean isEmailTokenExpired(String token) {
      try {
         getEmail(token);
         return false;
      } catch (Exception e) {
         return true;
      }
   }

   public boolean isTokenInvalidated(String token) {
      return tokenRepository.isTokenInvalidated(token);
   }

}
