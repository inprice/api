package io.inprice.scrapper.api.rest.service;

import com.google.gson.Gson;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Cryptor;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.AuthRepository;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.LoginDTOValidator;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import jodd.util.BCrypt;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.Base64;
import java.util.Date;
import java.util.List;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
    private final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);

    private final Properties properties = Beans.getSingleton(Properties.class);

    public ServiceResponse<AuthUser> login(LoginDTO loginDTO, Response response) {
        ServiceResponse<AuthUser> res = validate(loginDTO);
        if (res.isOK()) {
            ServiceResponse<User> findingRes = userRepository.findByEmail(loginDTO.getEmail(), true);
            if (findingRes.isOK()) {
                User user = findingRes.getModel();
                String salt = user.getPasswordSalt();
                String hash = BCrypt.hashpw(loginDTO.getPassword(), salt);
                if (hash.equals(user.getPasswordHash())) {
                    AuthUser authUser = new AuthUser();
                    authUser.setId(user.getId());
                    authUser.setEmail(user.getEmail());
                    authUser.setFullName(user.getFullName());
                    authUser.setType(user.getUserType());
                    authUser.setCompanyId(user.getCompanyId());
                    authUser.setWorkspaceId(user.getDefaultWorkspaceId());

                    response.header(Consts.Auth.AUTHORIZATION_HEADER, Consts.Auth.TOKEN_PREFIX + newToken(authUser));

                    res.setResult("OK");
                    res.setStatus(HttpStatus.OK_200);
                } else {
                    res.setStatus(HttpStatus.NOT_FOUND_404);
                    res.setResult("Invalid email or password!");
                }
            } else {
                res.setStatus(findingRes.getStatus());
                res.setResult(findingRes.getResult());
            }
        }
        return res;
    }

    public ServiceResponse refresh(Request req, Response res) {
        ServiceResponse serRes = new ServiceResponse(HttpStatus.UNAUTHORIZED_401);

        final String token = getToken(req);

        if (StringUtils.isBlank(token)) {
            serRes.setResult("Missing header: " + Consts.Auth.AUTHORIZATION_HEADER);
        } else if (isTokenInvalidated(token)) {
            serRes.setResult("Invalid token!");
        } else {
            revokeToken(token);
            AuthUser authUser = getAuthUser(token);
            res.header(Consts.Auth.AUTHORIZATION_HEADER, Consts.Auth.TOKEN_PREFIX + newToken(authUser));

            serRes.setStatus(HttpStatus.OK_200);
            serRes.setResult("OK");
        }

        return serRes;
    }

    public String newToken(AuthUser authUser) {
        Date now = new Date();

        final String prePayload = Global.gson.toJson(authUser);
        final byte[] payload = Cryptor.encrypt(prePayload);

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
        if (! StringUtils.isBlank(token)) authRepository.invalidateToken(token);
    }

    public AuthUser getAuthUser(Request req) {
        String token = getToken(req);
        if (token != null) {
            return getAuthUser(token);
        }
        return null;
    }

    public final AuthUser getAuthUser(String token) {
        Claims claims =
            Jwts.parser()
                .setSigningKey(Consts.Auth.APP_SECRET_KEY)
                .parseClaimsJws(token)
            .getBody();

        final byte[] prePayload = Base64.getDecoder().decode(claims.get(Consts.Auth.PAYLOAD, String.class));
        final String payload = Cryptor.decrypt(prePayload);

        return Global.gson.fromJson(payload, AuthUser.class);
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

    public boolean isTokenInvalidated(String token) {
        return authRepository.isTokenInvalidated(token);
    }

    private ServiceResponse<AuthUser> validate(LoginDTO loginDTO) {
        ServiceResponse<AuthUser> res = new ServiceResponse<>(HttpStatus.BAD_REQUEST_400);
        List<Problem> problems = LoginDTOValidator.verify(loginDTO);

        if (problems.size() > 0) {
            res.setProblems(problems);
        } else {
            res = new ServiceResponse<>(HttpStatus.OK_200);
        }
        return res;
    }

}
