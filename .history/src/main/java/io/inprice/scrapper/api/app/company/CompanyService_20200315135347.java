package io.inprice.scrapper.api.app.company;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.AuthService;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;

public class CompanyService {

   private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

   private static final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
   private static final AuthService authService = Beans.getSingleton(AuthService.class);

   public ServiceResponse findById(Long id) {
      return repository.findById(id);
   }

   public ServiceResponse insert(CompanyDTO companyDTO, String ip, String userAgent) {
      if (companyDTO != null) {
         ServiceResponse res = validate(companyDTO, true);
         if (res.isOK()) {
            res = repository.insert(companyDTO);
            if (res.isOK()) {
               log.info("A new company has been added successfully. " + companyDTO);
               LoginDTO loginDTO = new LoginDTO();
               loginDTO.setIp(ip);
               loginDTO.setUserAgent(userAgent);
               loginDTO.setEmail(companyDTO.getEmail());
               loginDTO.setPassword(companyDTO.getPassword());
               res = authService.login(loginDTO);
            }
         }
         return res;
      }
      return Responses.Invalid.COMPANY;
   }

   public ServiceResponse update(CompanyDTO companyDTO) {
      if (companyDTO != null) {
         if (companyDTO.getId() == null || companyDTO.getId() < 1) {
            return Responses.NotFound.COMPANY;
         }

         ServiceResponse res = validate(companyDTO, false);
         if (res.isOK()) {
            res = repository.update(companyDTO);
         }
         return res;
      }
      return Responses.Invalid.COMPANY;
   }

   private ServiceResponse validate(CompanyDTO companyDTO, boolean insert) {
      // only admins can update their companies
      if (!insert && (!Role.ADMIN.equals(Context.getAuthUser().getRole())
            || !companyDTO.getId().equals(Context.getCompanyId()))) {
         return Responses.PermissionProblem.UNAUTHORIZED;
      }

      List<String> problems = new ArrayList<>();

      if (insert) {
         problems = UserDTOValidator.verify(companyDTO, true, "Contact");
      }

      if (StringUtils.isBlank(companyDTO.getCompanyName())) {
         problems.add("Company name cannot be null!");
      } else if (companyDTO.getCompanyName().length() < 3 || companyDTO.getCompanyName().length() > 250) {
         problems.add("Company name must be between 3 and 250 chars!");
      }

      if (companyDTO.getCountry() == null) {
         problems.add("You should pick a country!");
      } else if (StringUtils.isBlank(companyDTO.getCountry())) {
         problems.add("Unknown country!");
      }

      if (companyDTO.getSector() == null) {
         problems.add("You should pick a sector!");
      } else if (StringUtils.isBlank(companyDTO.getSector())) {
         problems.add("Unknown sector!");
      }

      return Commons.createResponse(problems);
   }

}
