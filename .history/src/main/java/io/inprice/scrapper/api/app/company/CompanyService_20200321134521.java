package io.inprice.scrapper.api.app.company;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.NameAndEmailValidator;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Props;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;

public class CompanyService {

   private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

   private static final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);
   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private static final TokenService tokenService = Beans.getSingleton(TokenService.class);

   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);

   public ServiceResponse findById(Long id) {
      return companyRepository.findById(id);
   }

   public ServiceResponse registerRequest(RegisterDTO dto, String ip) {
      if (RedisClient.isIpRateLimited(RateLimiterType.REGISTER, ip)) {
         return Responses.Illegal.TOO_MUCH_REQUEST;
      }
      RedisClient.addIpToRateLimiter(RateLimiterType.REGISTER, ip);

      ServiceResponse res = validate(dto);
      if (res.isOK()) {

         ServiceResponse found = userRepository.findByEmail(dto.getEmail());
         if (found.isOK()) {
            User user = found.getData();
            dto.setUserId(user.getId());
            dto.setUserName(user.getName());
         }

         final String token = tokenService.getRegisterRequestToken(dto);

         try {
            Map<String, Object> dataMap = new HashMap<>(4);
            dataMap.put("userName", dto.getUserName());
            dataMap.put("companyName", dto.getCompanyName());
            dataMap.put("token", token);
            dataMap.put("baseUrl", Props.getFrontendBaseUrl());

            final String message = renderer.renderRegisterActivationLink(dataMap);
            emailSender.send(Props.getEmail_Sender(), "Register a new company", dto.getEmail(), message);

            return Responses.OK;
         } catch (Exception e) {
            log.error("An error occurred in rendering email for activating register company", e);
            return Responses.ServerProblem.EXCEPTION;
         }
      }
      return Responses.Invalid.COMPANY;
   }

   public ServiceResponse register(String requestToken, String ip) {
      if (dto != null) {
         ServiceResponse res = validate(dto, true);
         if (res.isOK()) {
            res = repository.insert(dto);
            if (res.isOK()) {
               log.info("A new company has been added successfully. " + dto);
               LoginDTO loginDTO = new LoginDTO();
               loginDTO.setIp(ip);
               loginDTO.setUserAgent(userAgent);
               loginDTO.setEmail(dto.getEmail());
               loginDTO.setPassword(dto.getPassword());
               res = authService.login(loginDTO);
            }
         }
         return res;
      }
      return Responses.Invalid.COMPANY;
   }

   public ServiceResponse update(CompanyDTO dto) {
      if (dto != null) {
         if (dto.getId() == null || dto.getId() < 1) {
            return Responses.NotFound.COMPANY;
         }

         ServiceResponse res = validate(dto, false);
         if (res.isOK()) {
            res = repository.update(dto);
         }
         return res;
      }
      return Responses.Invalid.COMPANY;
   }

   private ServiceResponse validate(RegisterDTO dto) {
      String problem = null;

      if (dto == null) {
         problem = "Invalid company info!";
      }

      if (problem == null) {
         problem = NameAndEmailValidator.verify(dto.getUserName(), dto.getEmail());
      }

      if (problem == null) {
         if (StringUtils.isBlank(dto.getCompanyName())) {
            problem = "Company name cannot be null!";
         } else if (dto.getCompanyName().length() < 3 || dto.getCompanyName().length() > 70) {
            problem = "Company name must be between 3 and 70 chars!";
         }
      }

      if (problem == null && StringUtils.isBlank(dto.getCountry())) {
         problem = "You should pick a country!";
      }

      if (problem == null && StringUtils.isNotBlank(dto.getSector())) {
         if (dto.getSector().length() < 2 || dto.getSector().length() > 50) {
            problem = "Sector must be between 2 and 50 chars!";
         }
      }

      if (problem == null && StringUtils.isNotBlank(dto.getWebsite())) {
         if (dto.getWebsite().length() < 10 || dto.getWebsite().length() > 100) {
            problem = "Website must be between 10 and 100 chars!";
         }
      }

      if (problem == null)
         return Responses.OK;
      else
         return new ServiceResponse(problem);
   }

}
