package io.inprice.scrapper.api.app.auth;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import io.inprice.scrapper.api.consts.Global;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;

public class SessionHelper {

   private static final String PAYLOAD = "payload";
   private static final String SECRET_KEY = "-8'fq{>As@n77jcx24.U*$=PS]#Z5wY+";

   static String toToken(List<SessionInfoForToken> sessions) {
      DefaultClaims claims = new DefaultClaims();
      claims.setIssuedAt(new Date());
      claims.put(PAYLOAD, sessions);
      return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
   }   

   public static List<SessionInfoForToken> fromToken(String token) {
      try {
         Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
         Object raw = claims.get(PAYLOAD, List.class);
         return Global.getObjectMapper().convertValue(raw, new TypeReference<List<SessionInfoForToken>>() {});
      } catch (Exception ignored) { 
         ignored.printStackTrace();
      }
      return null;
   }

}