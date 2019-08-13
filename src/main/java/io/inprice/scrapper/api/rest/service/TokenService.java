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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.util.Base64;
import java.util.Date;

//TODO: some methods must be added to provide expired tokens for testing
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final TokenRepository tokenRepository = Beans.getSingleton(TokenRepository.class);
    private final Properties properties = Beans.getSingleton(Properties.class);

    public String newToken(AuthUser authUser) {
        final String payload = Global.gson.toJson(authUser);
        return generateToken(Cryptor.encrypt(payload));
    }

    public String newTokenEmailFor(String email) {
        return generateToken(Cryptor.encrypt(email));
    }

    private String generateToken(byte[] payload) {
        Date now = new Date();

        DefaultClaims claims = new DefaultClaims();
        claims.put(Consts.Auth.PAYLOAD, payload);
        claims.put(Consts.Auth.ISSUED_AT, now.getTime()); //this property provides uniqueness to all tokens (even for the same user)

        return
            Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + properties.getTTL_Tokens() * 60 * 1000L))
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

    public boolean validateToken(String token) {
        if (! isTokenInvalidated(token)) {
            try {
                getAuthUser(token);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean validateEmailToken(String token) {
        if (! isTokenInvalidated(token)) {
            try {
                getEmail(token);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isTokenInvalidated(String token) {
        return tokenRepository.isTokenInvalidated(token);
    }

}
