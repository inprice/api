package io.inprice.scrapper.api.app.auth;

import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;

public class SessionHelper {

   private static final ObjectMapper mapper = new ObjectMapper();
   private static final String PAYLOAD = "payload";
   private static final String SECRET_KEY = "-8'fq{>As@n77jcx24.U*$=PS]#Z5wY+";

   static String toToken(AuthUser authUser) {
      DefaultClaims claims = new DefaultClaims();
      claims.setIssuedAt(new Date());
      claims.put(PAYLOAD, authUser);
      return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
   }   

   public static AuthUser fromToken(String token) {
      try {
         Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
         Object raw = claims.get(PAYLOAD);
         return mapper.convertValue(raw, AuthUser.class);
      } catch (Exception ignored) { 
         ignored.printStackTrace();
      }
      return null;
   }

}