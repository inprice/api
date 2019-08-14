package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Cryptor;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.rest.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.apache.commons.lang3.StringUtils;
import spark.Request;

import java.util.Base64;
import java.util.Date;

public class TokenService {

    private final TokenRepository tokenRepository = Beans.getSingleton(TokenRepository.class);
    private final Properties properties = Beans.getSingleton(Properties.class);

    public String newToken(AuthUser authUser) {
        final String payload = Global.gson.toJson(authUser);
        return Consts.Auth.TOKEN_PREFIX + generateToken(Cryptor.encrypt(payload));
    }

    public String newTokenEmailFor(String email) {
        return generateToken(Cryptor.encrypt(email));
    }

    private String generateToken(byte[] payload) {
        Date now = new Date();

        DefaultClaims claims = new DefaultClaims();
        claims.setIssuedAt(now);
        claims.setExpiration(new Date(now.getTime() + (properties.getTTL_TokensInSeconds() * 1000L)));
        claims.put(Consts.Auth.PAYLOAD, payload);

        return
            Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, Consts.Auth.APP_SECRET_KEY)
            .compact();
    }

    public void revokeToken(String token) {
        if (! StringUtils.isBlank(token)) tokenRepository.invalidateToken(token);
    }

    public AuthUser getAuthUser(Request req) {
        String token = getToken(req);
        if (token != null) {
            return getAuthUser(token);
        }
        return null;
    }

    public AuthUser getAuthUser(String token) {
        Claims claims =
            Jwts.parser()
                .setSigningKey(Consts.Auth.APP_SECRET_KEY)
                .parseClaimsJws(token)
            .getBody();

        final byte[] prePayload = Base64.getDecoder().decode(claims.get(Consts.Auth.PAYLOAD, String.class));
        final String payload = Cryptor.decrypt(prePayload);

        return Global.gson.fromJson(payload, AuthUser.class);
    }

    public String getEmail(String token) {
        Claims claims =
            Jwts.parser()
                .setSigningKey(Consts.Auth.APP_SECRET_KEY)
                .parseClaimsJws(token)
            .getBody();

        final byte[] prePayload = Base64.getDecoder().decode(claims.get(Consts.Auth.PAYLOAD, String.class));
        return Cryptor.decrypt(prePayload);
    }

    public String getToken(Request request) {
        String header = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
        if (header != null && header.length() > 0)
            return header.replace(Consts.Auth.TOKEN_PREFIX, "");
        else
            return null;
    }

    public AuthUser isTokenExpired(String token) {
        try {
            return getAuthUser(token);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEmailTokenExpired(String token) {
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
