package io.inprice.scrapper.api.rest.service;

import java.util.Base64;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Cryptor;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Tokens;
import io.inprice.scrapper.api.rest.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import spark.Request;

public class TokenService {

    private final TokenRepository tokenRepository = Beans.getSingleton(TokenRepository.class);

    Tokens generateTokens(AuthUser authUser, String ip, String userAgent) {
        String refresh = authUser.getEmail() + "::"  + ip + "::" + Consts.Auth.APP_SECRET_KEY + "::" + userAgent;
        String refreshToken = generateToken(false, Cryptor.encrypt(refresh));

    	String access = Global.gson.toJson(authUser);
    	String accessToken = generateToken(true, Cryptor.encrypt(access));

    	return new Tokens(accessToken, refreshToken);
    }

    String newTokenEmailFor(String email) {
        return generateToken(true, Cryptor.encrypt(email));
    }

    private String generateToken(boolean isAccessToken, byte[] payload) {
        Date now = new Date();

        DefaultClaims claims = new DefaultClaims();
        claims.setIssuedAt(now);
        claims.setExpiration(new Date(now.getTime() + (isAccessToken ? Props.getTTL_AccessTokens() : Props.getTTL_RefreshTokens())));
        claims.put(Consts.Auth.PAYLOAD, payload);

        return
            Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, Consts.Auth.APP_SECRET_KEY)
            .compact();
    }

    void revokeToken(String token) {
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
        return Global.gson.fromJson(getTokenString(token), AuthUser.class);
    }
    
    public String getEmail(String token) {
    	return getTokenString(token);
    }

	public String getRefreshString(String token) {
    	return getTokenString(token);
    }

    private String getTokenString(String token) {
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

    public AuthUser isAccessTokenExpired(String token) {
        try {
            return getAuthUser(token);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isRefreshTokenExpiredOrSuspicious(String token, String ip, String userAgent) {
        try {
        	//refresh request must be done from the same ip and device as is it is in the first request
        	//so ip and user-agent fields are checked for these controls
            String bareToken = getTokenString(token);
            String[] tokenParts = bareToken.split("::");
            return (! ip.equals(tokenParts[1]) || ! userAgent.equals(tokenParts[3]));
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
