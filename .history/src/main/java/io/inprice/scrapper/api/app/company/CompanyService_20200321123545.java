package io.inprice.scrapper.api.app.company;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.util.ElementScanner6;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.AuthService;
import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.NameAndEmailValidator;
import io.inprice.scrapper.api.dto.UserDTOValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;

public class CompanyService {

   private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

   private static final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
   private static final UserRepository repository = Beans.getSingleton(CompanyRepository.class);
   private static final MemberRepository repository = Beans.getSingleton(CompanyRepository.class);
   private static final AuthService authService = Beans.getSingleton(AuthService.class);

   public ServiceResponse findById(Long id) {
      return repository.findById(id);
   }

   public ServiceResponse registerRequest(RegisterDTO dto, String ip, String userAgent) {
      if (RedisClient.isIpRateLimited(RateLimiterType.REGISTER, ip)) {
         return Responses.Illegal.TOO_MUCH_REQUEST;
      }
      RedisClient.addIpToRateLimiter(RateLimiterType.REGISTER, ip);

      ServiceResponse res = validate(dto);
      if (dto != null) {
         if (res.isOK()) {
            res = authService.login(loginDTO);
         }
         return res;
      }
      return Responses.Invalid.COMPANY;
   }

   public ServiceResponse register(RegisterDTO dto, String ip, String userAgent) {
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
