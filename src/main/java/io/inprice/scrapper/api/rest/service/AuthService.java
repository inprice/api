package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import io.inprice.scrapper.api.rest.validator.EmailDTOValidator;
import io.inprice.scrapper.api.rest.validator.LoginDTOValidator;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;
import org.apache.commons.lang3.StringUtils;
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
    private final WorkspaceRepository workspaceRepository = Beans.getSingleton(WorkspaceRepository.class);
    private final TokenService tokenService = Beans.getSingleton(TokenService.class);
    private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
    private final Properties props = Beans.getSingleton(Properties.class);
    private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

    public ServiceResponse login(LoginDTO loginDTO, Response response) {
        if (loginDTO != null) {
            ServiceResponse res = validate(loginDTO);
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
                        authUser.setAllowedWorkspaces(workspaceRepository.findByCompanyId(user.getCompanyId()));

                        response.header(Consts.Auth.AUTHORIZATION_HEADER, tokenService.newToken(authUser));

                        return Responses.OK;
                    } else {
                        return Responses.NotFound.USER;
                    }
                } else {
                    return findingRes;
                }
            }
            return res;
        }
        return Responses.Invalid.EMAIL_OR_PASSWORD;
    }

    public ServiceResponse forgotPassword(EmailDTO emailDTO) {
        if (emailDTO != null) {
            ServiceResponse res = validateEmail(emailDTO);
            if (res.isOK()) {
                ServiceResponse<User> found = userRepository.findByEmail(emailDTO.getEmail());
                if (found.isOK()) {

                    final String token = tokenService.newTokenEmailFor(emailDTO.getEmail());
                    try {
                        if (props.isRunningForTests()) {
                            return new ServiceResponse(token); //--> for test purposes, we need this info to test some functionality during testing
                        } else {
                            Map<String, Object> dataMap = new HashMap<>(2);
                            dataMap.put("fullName", found.getModel().getFullName());
                            dataMap.put("token", token);

                            final String message = renderer.renderForgotPassword(dataMap);
                            emailSender.send(props.getEmail_Sender(), "Reset your password", found.getModel().getEmail(), message);
                            return Responses.OK;
                        }
                    } catch (Exception e) {
                        log.error("An error occurred in rendering email for forgetting password", e);
                        return Responses.ServerProblem.EXCEPTION;
                    }
                } else {
                    return Responses.NotFound.EMAIL;
                }
            }
            return res;
        }
        return Responses.Invalid.PASSWORD;
    }

    public ServiceResponse resetPassword(PasswordDTO passwordDTO) {
        if (passwordDTO != null) {
            ServiceResponse res = validatePassword(passwordDTO);
            if (res.isOK()) {
                final String email = tokenService.getEmail(passwordDTO.getToken());

                ServiceResponse<User> found = userRepository.findByEmail(email);
                if (found.isOK()) {
                    AuthUser authUser = new AuthUser();
                    authUser.setId(found.getModel().getId());
                    authUser.setCompanyId(found.getModel().getCompanyId());
                    res = userRepository.updatePassword(passwordDTO, authUser);
                    tokenService.revokeToken(passwordDTO.getToken());
                } else {
                    return Responses.NotFound.EMAIL;
                }
            }
            return res;
        }

        return Responses.Invalid.PASSWORD;
    }

    public ServiceResponse refresh(Request req, Response res) {
        final String token = tokenService.getToken(req);

        if (StringUtils.isBlank(token)) {
            return Responses.Missing.AUTHORIZATION_HEADER;
        } else if (tokenService.isTokenInvalidated(token)) {
            return Responses.Invalid.TOKEN;
        } else {
            tokenService.revokeToken(token);
            AuthUser authUser = tokenService.getAuthUser(token);
            res.header(Consts.Auth.AUTHORIZATION_HEADER, tokenService.newToken(authUser));
            return Responses.OK;
        }
    }

    public ServiceResponse logout(Request req) {
        tokenService.revokeToken(tokenService.getToken(req));
        return Responses.OK;
    }

    private ServiceResponse validateEmail(EmailDTO emailDTO) {
        List<Problem> problems = new ArrayList<>();
        EmailDTOValidator.verify(emailDTO.getEmail(), problems);
        return Commons.createResponse(problems);
    }

    private ServiceResponse validatePassword(PasswordDTO passwordDTO) {
        List<io.inprice.scrapper.api.info.Problem> problems = PasswordDTOValidator.verify(passwordDTO, true, false);

        if (StringUtils.isBlank(passwordDTO.getToken())) {
            problems.add(new io.inprice.scrapper.api.info.Problem("form", "Token cannot be null!"));
        } else if (tokenService.isTokenInvalidated(passwordDTO.getToken())) {
            problems.add(new io.inprice.scrapper.api.info.Problem("form", "Invalid token!"));
        } else if (tokenService.isEmailTokenExpired(passwordDTO.getToken())) {
            problems.add(new io.inprice.scrapper.api.info.Problem("form", "Expired token!"));
        }

        return Commons.createResponse(problems);
    }

    private ServiceResponse validate(LoginDTO loginDTO) {
        List<Problem> problems = LoginDTOValidator.verify(loginDTO);
        return Commons.createResponse(problems);
    }

}
