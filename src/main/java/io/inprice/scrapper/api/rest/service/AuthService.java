package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.LoggedOutTokensRepository;
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

import java.util.Date;
import java.util.List;

public class AuthService {

    private final LoggedOutTokensRepository loggedOutTokensRepository = new LoggedOutTokensRepository();
    private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

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

    //todo: must be called within a periodic job
    public final void removeExpired() {
        loggedOutTokensRepository.removeExpired();
    }

    public final String newToken(AuthUser authUser) {
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject(authUser.getEmail());
        claims.put(Consts.Auth.USER_ID, authUser.getId());
        claims.put(Consts.Auth.USER_TYPE, authUser.getType());
        claims.put(Consts.Auth.USER_FULL_NAME, authUser.getFullName());
        claims.put(Consts.Auth.COMPANY_ID, authUser.getCompanyId());
        claims.put(Consts.Auth.WORKSPACE_ID, authUser.getWorkspaceId());

        return
            Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + Consts.Auth.TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, Consts.Auth.SECRET_KEY)
            .compact();
    }

    public final void revokeToken(String token) {
        Date expirationDate = Jwts.parser()
                .setSigningKey(Consts.Auth.SECRET_KEY)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        loggedOutTokensRepository.addToken(token, expirationDate.getTime());
    }

    public final AuthUser getAuthenticatedUser(String token) {
        Claims claims =
            Jwts.parser()
                .setSigningKey(Consts.Auth.SECRET_KEY)
                .parseClaimsJws(token)
            .getBody();

        return
            new AuthUser(
                claims.get(Consts.Auth.USER_ID, Long.class),
                claims.getSubject(),
                claims.get(Consts.Auth.USER_TYPE, UserType.class),
                claims.get(Consts.Auth.USER_FULL_NAME, String.class),
                claims.get(Consts.Auth.COMPANY_ID, Long.class),
                claims.get(Consts.Auth.WORKSPACE_ID, Long.class)
            );
    }

    public final boolean isTokenLoggedOut(String token) {
        return loggedOutTokensRepository.isTokenLoggedOut(token);
    }

    public final boolean validateToken(String token) {
        if (! isTokenLoggedOut(token)) {
            try {
                getAuthenticatedUser(token);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private ServiceResponse validate(LoginDTO loginDTO) {
        ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
        List<Problem> problems = LoginDTOValidator.verify(loginDTO);

        if (problems.size() > 0) {
            res.setProblems(problems);
        } else {
            res = InstantResponses.OK;
        }
        return res;
    }

}
