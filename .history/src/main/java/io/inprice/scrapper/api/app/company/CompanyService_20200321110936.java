package io.inprice.scrapper.api.app.company;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.AuthService;
import io.inprice.scrapper.api.component.Commons;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.UserDTOValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class CompanyService {

   private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

   private static final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
   private static final AuthService authService = Beans.getSingleton(AuthService.class);

   public ServiceResponse findById(Long id) {
      return repository.findById(id);
   }

   public ServiceResponse insert(CompanyDTO dto, String ip, String userAgent) {
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

   private ServiceResponse validate(CompanyDTO dto, boolean insert) {
      // only admins can update their companies
      if (!insert && (!Role.ADMIN.equals(Context.getAuthUser().getRole())
            || !dto.getId().equals(Context.getCompanyId()))) {
         return Responses.PermissionProblem.UNAUTHORIZED;
      }

      List<String> problems = new ArrayList<>();

      if (insert) {
         problems = UserDTOValidator.verify(dto, true, "Contact");
      }

      if (StringUtils.isBlank(dto.getCompanyName())) {
         problems.add("Company name cannot be null!");
      } else if (dto.getCompanyName().length() < 3 || dto.getCompanyName().length() > 250) {
         problems.add("Company name must be between 3 and 250 chars!");
      }

      if (dto.getCountry() == null) {
         problems.add("You should pick a country!");
      } else if (StringUtils.isBlank(dto.getCountry())) {
         problems.add("Unknown country!");
      }

      if (dto.getSector() == null) {
         problems.add("You should pick a sector!");
      } else if (StringUtils.isBlank(dto.getSector())) {
         problems.add("Unknown sector!");
      }

      return Commons.createResponse(problems);
   }

}
