package io.inprice.scrapper.api.app.auth;

import java.util.Base64;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.consts.Global;
import io.inprice.scrapper.api.helpers.Cryptor;
import io.inprice.scrapper.api.info.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.DefaultClaims;

public class SessionHelper {

   private static final String PAYLOAD = "payload";
   private static final String SECRET_KEY = "-8'fq{>As@n77jcx24.U*$=PS]#Z5wY+";

   static String toToken(AuthUser authUser) {
      if (authUser != null) {
         String json = Global.gson.toJson(authUser);
         return generateToken(Cryptor.encrypt(json));
      }
      return null;
   }

   public static AuthUser fromToken(String token) {
      if (StringUtils.isNotBlank(token)) {
         try {
            String json = decryptToken(token);
            return Global.gson.fromJson(json, AuthUser.class);
         } catch (Exception ignored) { }
      }
      return null;
   }

   private static String decryptToken(String token) {
      try {
         Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
         byte[] prePayload = Base64.getDecoder().decode(claims.get(PAYLOAD, String.class));
         return Cryptor.decrypt(prePayload);
      } catch (SignatureException ignored) { }
      return null;
   }

   private static String generateToken(byte[] payload) {
      DefaultClaims claims = new DefaultClaims();
      claims.setIssuedAt(new Date());
      claims.put(PAYLOAD, payload);
      return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
   }   

}