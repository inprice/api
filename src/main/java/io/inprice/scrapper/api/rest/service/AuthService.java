package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.EmailDTOValidator;
import io.inprice.scrapper.api.rest.validator.LoginDTOValidator;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
    private final TokenService tokenService = Beans.getSingleton(TokenService.class);
    private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
    private final Properties properties = Beans.getSingleton(Properties.class);
    private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

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

                    response.header(Consts.Auth.AUTHORIZATION_HEADER, tokenService.newToken(authUser));

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

    public ServiceResponse forgotPassword(EmailDTO emailDTO) {
        ServiceResponse res = validateEmail(emailDTO);
        if (res.isOK()) {
            ServiceResponse<User> found = userRepository.findByEmail(emailDTO.getEmail());
            if (found.isOK()) {

                final String token = tokenService.newTokenEmailFor(emailDTO.getEmail());
                try {
                    if (properties.isRunningForTests()) {
                        res.setResult(token); //--> for test purposes, we need this info to test some functionality during testing
                    } else {
                        Map<String, Object> dataMap = new HashMap<>(2);
                        dataMap.put("fullName", found.getModel().getFullName());
                        dataMap.put("token", token);

                        final String message = renderer.renderForgotPassword(dataMap);
                        emailSender.send(properties.getEmail_Sender(), "Reset your password", found.getModel().getEmail(), message);

                        res.setResult("OK");
                    }
                    res.setStatus(HttpStatus.OK_200);
                } catch (Exception e) {
                    log.error("An error occurred in rendering email for forgetting password", e);
                    res = InstantResponses.SERVER_ERROR(e);
                }
            } else {
                res = InstantResponses.NOT_FOUND("Email");
            }
        }
        return res;
    }

    public ServiceResponse resetPassword(PasswordDTO passwordDTO) {
        ServiceResponse res = validatePassword(passwordDTO);
        if (res.isOK()) {
            final String email = tokenService.getEmail(passwordDTO.getToken());

            ServiceResponse<User> found = userRepository.findByEmail(email);
            if (found.isOK()) {
                AuthUser authUser = new AuthUser();
                authUser.setId(found.getModel().getId());
                authUser.setCompanyId(found.getModel().getCompanyId());
                res = userRepository.updatePassword(passwordDTO, authUser);
            } else {
                res = InstantResponses.NOT_FOUND("Email");
            }
        }
        return res;
    }

    public ServiceResponse refresh(Request req, Response res) {
        ServiceResponse serRes = new ServiceResponse(HttpStatus.UNAUTHORIZED_401);

        final String token = tokenService.getToken(req);

        if (StringUtils.isBlank(token)) {
            serRes.setResult("Missing header: " + Consts.Auth.AUTHORIZATION_HEADER);
        } else if (tokenService.isTokenInvalidated(token)) {
            serRes.setResult("Invalid token!");
        } else {
            tokenService.revokeToken(token);
            AuthUser authUser = tokenService.getAuthUser(token);
            res.header(Consts.Auth.AUTHORIZATION_HEADER, tokenService.newToken(authUser));

            serRes.setStatus(HttpStatus.OK_200);
            serRes.setResult("OK");
        }

        return serRes;
    }

    public ServiceResponse logout(Request req) {
        tokenService.revokeToken(tokenService.getToken(req));
        return InstantResponses.OK;
    }

    private ServiceResponse validateEmail(EmailDTO emailDTO) {
        ServiceResponse res = new ServiceResponse<>(HttpStatus.BAD_REQUEST_400);

        List<Problem> problems = new ArrayList<>();
        boolean isValid = EmailDTOValidator.verify(emailDTO.getEmail(), problems);

        if (! isValid) {
            res.setProblems(problems);
        } else {
            res = new ServiceResponse<>(HttpStatus.OK_200);
        }
        return res;
    }

    private ServiceResponse validatePassword(PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(passwordDTO, true, false);

        if (StringUtils.isBlank(passwordDTO.getToken())) {
            problems.add(new Problem("form", "Token cannot be null!"));
        } else if (tokenService.isTokenInvalidated(passwordDTO.getToken())) {
            problems.add(new Problem("form", "Invalid token!"));
        } else if (tokenService.isEmailTokenExpired(passwordDTO.getToken())) {
            problems.add(new Problem("form", "Expired token!"));
        }

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
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
