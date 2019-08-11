package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.controller.AuthController;
import io.inprice.scrapper.api.rest.repository.InvalidatedTokensRepository;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.LoginDTOValidator;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import jodd.util.BCrypt;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.util.Date;
import java.util.List;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
    private final InvalidatedTokensRepository invalidatedTokensRepository = Beans.getSingleton(InvalidatedTokensRepository.class);

    private final Properties properties = Beans.getSingleton(Properties.class);

    public ServiceResponse<AuthUser> login(LoginDTO loginDTO) {
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

                    res.setStatus(HttpStatus.OK_200);
                    res.setModel(authUser);
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

    public final String newToken(AuthUser authUser) {
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject(authUser.getEmail());
        claims.put(Consts.Auth.USER_ID, authUser.getId());
        claims.put(Consts.Auth.USER_TYPE, authUser.getType());
        claims.put(Consts.Auth.USER_FULL_NAME, authUser.getFullName());
        claims.put(Consts.Auth.COMPANY_ID, authUser.getCompanyId());
        claims.put(Consts.Auth.WORKSPACE_ID, authUser.getWorkspaceId());

        Date now = new Date();

        return
            Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + properties.getTTL_Tokens() * 60 * 1000L))
                .signWith(SignatureAlgorithm.HS512, Consts.Auth.SECRET_KEY)
            .compact();
    }

    public final void revokeToken(String token) {
        invalidatedTokensRepository.addToken(token);
    }

    public final AuthUser getAuthUser(Request req) {
        String token = getToken(req);
        if (token != null) {
            return getAuthUser(token);
        }
        return null;
    }

    public final AuthUser getAuthUser(String token) {
        Claims claims =
            Jwts.parser()
                .setSigningKey(Consts.Auth.SECRET_KEY)
                .parseClaimsJws(token)
            .getBody();

        return
            new AuthUser(
                claims.get(Consts.Auth.USER_ID, Long.class),
                claims.getSubject(),
                UserType.valueOf(claims.get(Consts.Auth.USER_TYPE, String.class)),
                claims.get(Consts.Auth.USER_FULL_NAME, String.class),
                claims.get(Consts.Auth.COMPANY_ID, Long.class),
                claims.get(Consts.Auth.WORKSPACE_ID, Long.class)
            );
    }

    public String getToken(Request request) {
        String header = request.headers(Consts.Auth.AUTHORIZATION_HEADER);
        if (header != null && header.length() > 0)
            return header.replace(Consts.Auth.TOKEN_PREFIX, "");
        else
            return null;
    }

    public final boolean validateToken(String token) {
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

    public final boolean isTokenInvalidated(String token) {
        return invalidatedTokensRepository.isTokenInvalidated(token);
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
