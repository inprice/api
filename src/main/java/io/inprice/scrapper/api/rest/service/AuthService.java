package io.inprice.scrapper.api.rest.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.info.Tokens;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.EmailDTOValidator;
import io.inprice.scrapper.api.rest.validator.LoginDTOValidator;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;
import spark.Request;

public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
	private final TokenService tokenService = Beans.getSingleton(TokenService.class);
	private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
	private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

	public ServiceResponse login(LoginDTO loginDTO) {
		if (loginDTO != null) {
			ServiceResponse res = validate(loginDTO);
			if (res.isOK()) {
				ServiceResponse findingRes = userRepository.findByEmail(loginDTO.getEmail(), true);
				if (findingRes.isOK()) {
					User user = findingRes.getData();
					String salt = user.getPasswordSalt();
					String hash = BCrypt.hashpw(loginDTO.getPassword(), salt);
					if (hash.equals(user.getPasswordHash())) {
						user.setPasswordSalt(null);
						user.setPasswordHash(null);

						Tokens tokens = createTokens(user, loginDTO.getIp(), loginDTO.getUserAgent());
						Map<String, Object> data = new HashMap<>(2);
						data.put("user", user);
						data.put("tokens", tokens);
						return new ServiceResponse(data);
					}
				}
			}
		}
		return Responses.Invalid.EMAIL_OR_PASSWORD;
	}

	public ServiceResponse forgotPassword(EmailDTO emailDTO) {
		if (emailDTO != null) {
			if (RedisClient.getForgotpasswordemails().contains(emailDTO.getEmail())) {
				return Responses.Illegal.TOO_MUCH_REQUEST;
			}
			RedisClient.getForgotpasswordemails().add(emailDTO.getEmail(), Props.getAPP_WaitingTime(), TimeUnit.MINUTES);

			ServiceResponse res = validateEmail(emailDTO);
			if (res.isOK()) {
				ServiceResponse found = userRepository.findByEmail(emailDTO.getEmail());
				if (found.isOK()) {

					final String token = tokenService.newTokenEmailFor(emailDTO.getEmail());
					try {
						if (Props.isRunningForTests()) {
							return new ServiceResponse(token); // --> for test purposes, we need this info to test some
																// functionality during testing
						} else {
							User user = found.getData();
							Map<String, Object> dataMap = new HashMap<>(3);
							dataMap.put("fullName", user.getFullName());
							dataMap.put("token", token);
							dataMap.put("baseUrl", Props.getFrontendBaseUrl());

							final String message = renderer.renderForgotPassword(dataMap);
							emailSender.send(Props.getEmail_Sender(), "Reset your password", user.getEmail(), message);
						}
					} catch (Exception e) {
						log.error("An error occurred in rendering email for forgetting password", e);
						return Responses.ServerProblem.EXCEPTION;
					}
				}
			}
			return res;
		}
		return Responses.Invalid.EMAIL;
	}

	public ServiceResponse resetPassword(PasswordDTO passwordDTO) {
		if (passwordDTO != null) {
			ServiceResponse res = validatePassword(passwordDTO);
			if (res.isOK()) {
				if (!tokenService.isTokenInvalidated(passwordDTO.getToken())) {
					final String email = tokenService.getEmail(passwordDTO.getToken());

					ServiceResponse found = userRepository.findByEmail(email);
					if (found.isOK()) {
						User user = found.getData();
						AuthUser authUser = new AuthUser();
						authUser.setId(user.getId());
						authUser.setCompanyId(user.getCompanyId());
						authUser.setWorkspaceId(user.getWorkspaceId());
						res = userRepository.updatePassword(passwordDTO, authUser);
						tokenService.revokeToken(passwordDTO.getToken());
					} else {
						return Responses.NotFound.EMAIL;
					}
				} else {
					return Responses.Invalid.TOKEN;
				}
			}
			return res;
		}

		return Responses.Invalid.PASSWORD;
	}

	public ServiceResponse refreshTokens(String token, String ip, String userAgent) {
		if (!StringUtils.isBlank(token) && !tokenService.isTokenInvalidated(token)) {
			tokenService.revokeToken(token);
			String bareRefreshToken = tokenService.getRefreshString(token);
			String[] tokenParts = bareRefreshToken.split("::");
			ServiceResponse found = userRepository.findByEmail(tokenParts[0]);
			if (found.isOK()) {
				User user = found.getData();
				return new ServiceResponse(createTokens(user, ip, userAgent));
			}
		}
		return Responses.Invalid.TOKEN;
	}

	public ServiceResponse logout(Request req) {
		tokenService.revokeToken(tokenService.getToken(req));
		return Responses.OK;
	}

	private ServiceResponse validateEmail(EmailDTO emailDTO) {
		List<String> problems = new ArrayList<>();
		EmailDTOValidator.verify(emailDTO.getEmail(), problems);
		return Commons.createResponse(problems);
	}

	private ServiceResponse validatePassword(PasswordDTO passwordDTO) {
		List<String> problems = PasswordDTOValidator.verify(passwordDTO, true, false);

		if (StringUtils.isBlank(passwordDTO.getToken())) {
			problems.add("Token cannot be null!");
		} else if (tokenService.isEmailTokenExpired(passwordDTO.getToken())) {
			problems.add("Your token has expired!");
		}

		return Commons.createResponse(problems);
	}

	private ServiceResponse validate(LoginDTO loginDTO) {
		List<String> problems = LoginDTOValidator.verify(loginDTO);
		return Commons.createResponse(problems);
	}

	Tokens createTokens(User user, String ip, String userToken) {
		AuthUser authUser = new AuthUser();
		authUser.setId(user.getId());
		authUser.setEmail(user.getEmail());
		authUser.setFullName(user.getFullName());
		authUser.setRole(user.getRole());
		authUser.setCompanyId(user.getCompanyId());
		authUser.setWorkspaceId(user.getWorkspaceId());

		return createTokens(authUser, ip, userToken);
	}

	Tokens createTokens(AuthUser user, String ip, String userToken) {
		AuthUser authUser = new AuthUser();
		authUser.setId(user.getId());
		authUser.setEmail(user.getEmail());
		authUser.setFullName(user.getFullName());
		authUser.setRole(user.getRole());
		authUser.setCompanyId(user.getCompanyId());
		authUser.setWorkspaceId(user.getWorkspaceId());

		return tokenService.generateTokens(authUser, ip, userToken);
	}

}
