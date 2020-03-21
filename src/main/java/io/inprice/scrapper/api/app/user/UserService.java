package io.inprice.scrapper.api.app.user;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.UserStatus;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;

public class UserService {

   private static final Logger log = LoggerFactory.getLogger(UserService.class);
   private static final UserRepository repository = Beans.getSingleton(UserRepository.class);

   public ServiceResponse findById(Long id) {
      if (id != null && id > 0 && id < Integer.MAX_VALUE) {
         return repository.findById(id);
      }
      return Responses.Invalid.USER;
   }

   public ServiceResponse getList() {
      return repository.getList();
   }

   public ServiceResponse search(Map<String, String> searchMap) {
      SearchModel searchModel = new SearchModel(searchMap, User.class);
      return repository.search(searchModel);
   }

   public ServiceResponse insert(UserDTO userDTO) {
      if (userDTO != null) {
         userDTO.setStatus(UserStatus.PENDING);
         ServiceResponse res = validateUser(userDTO, true);
         if (res.isOK()) {
            res = repository.insert(userDTO);
            if (res.isOK()) {
               log.info("A new user has been added successfully. CompanyId: {}, Email: {}", Context.getCompanyId(),
                     userDTO.getEmail());
            }
         }
         return res;
      }
      return Responses.Invalid.USER;
   }

   public ServiceResponse update(UserDTO userDTO) {
      if (userDTO != null) {
         if (userDTO.getId() == null || userDTO.getId() < 1) {
            return Responses.NotFound.USER;
         }

         ServiceResponse res = validateUser(userDTO, false);
         if (res.isOK()) {
            res = repository.update(userDTO, true, true);
         }
         return res;
      }
      return Responses.Invalid.USER;
   }

   public ServiceResponse updatePassword(PasswordDTO passwordDTO) {
      if (passwordDTO != null) {
         if (passwordDTO.getId() == null || passwordDTO.getId() < 1) {
            return Responses.NotFound.USER;
         }

         ServiceResponse res = validatePassword(passwordDTO);
         if (res.isOK()) {
            res = repository.updatePassword(passwordDTO, Context.getAuthUser());
         }
         return res;
      }
      return Responses.Invalid.PASSWORD;
   }

   public ServiceResponse deleteById(Long id) {
      if (id != null && id > 0 && id < Integer.MAX_VALUE) {
         return repository.deleteById(id);
      }
      return Responses.Invalid.USER;
   }

   public ServiceResponse toggleStatus(Long id) {
      if (id != null && id > 0 && id < Integer.MAX_VALUE) {
         return repository.toggleStatus(id);
      }
      return Responses.Invalid.USER;
   }

   private ServiceResponse validateUser(UserDTO userDTO, boolean insert) {
      List<String> problems = UserDTOValidator.verify(userDTO, insert, "Full");
      return Commons.createResponse(problems);
   }

   private ServiceResponse validatePassword(PasswordDTO passwordDTO) {
      List<String> problems = PasswordDTOValidator.verify(passwordDTO, true, true);
      return Commons.createResponse(problems);
   }

}
