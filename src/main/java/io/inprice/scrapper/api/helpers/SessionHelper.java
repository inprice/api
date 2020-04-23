package io.inprice.scrapper.api.helpers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.core.type.TypeReference;

import io.inprice.scrapper.api.consts.Global;
import io.inprice.scrapper.api.session.info.ForCookie;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;

public class SessionHelper {

   private static final JwtBuilder builder;
   private static final JwtParser parser;

   static {
      SecretKey key =
         Keys.hmacShaKeyFor("-8'fq{>As@n77jcx24.U*$=PS]#Z5wY+".getBytes(StandardCharsets.UTF_8));

      builder = 
         Jwts.builder()
            .signWith(key)
            .serializeToJsonWith(
               new JacksonSerializer<>(Global.getObjectMapper())
            );

      parser = 
         Jwts.parserBuilder()
            .setSigningKey(key)
            .deserializeJsonWith(
               new JacksonDeserializer<>(Global.getObjectMapper())
            )
         .build();
   }

   public static String toToken(List<ForCookie> sessions) {
      DefaultClaims claims = new DefaultClaims();
      claims.put("payload", sessions);
      return builder.setClaims(claims).compact();
   }   

   public static List<ForCookie> fromToken(String token) {
      try {
         Claims claims = parser.parseClaimsJws(token).getBody();
         Object raw = claims.get("payload", List.class);
         return Global.getObjectMapper().convertValue(raw, new TypeReference<List<ForCookie>>() {});
      } catch (Exception ignored) { 
         ignored.printStackTrace();
      }
      return null;
   }

}