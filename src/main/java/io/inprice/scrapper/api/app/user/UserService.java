package io.inprice.scrapper.api.app.user;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;

/**
 * TODO:
 * Eklenmesi gereken fonksiyonlar
 * 1- Bir davete internal olarak confirm ya da reject verilebilmeli
 */
public class UserService {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

   public ServiceResponse updateName(String name) {
      if (StringUtils.isNotBlank(name)) {
         return Responses.Invalid.NAME;
      } else if (name.length() < 3 || name.length() > 70) {
         return new ServiceResponse("Name must be between 3 and 70 chars!");
      }

      return userRepository.updateName(name);
   }

   public ServiceResponse updatePassword(PasswordDTO dto) {
      String problem = PasswordValidator.verify(dto, true, true);
      if (problem == null) {
         return userRepository.updatePassword(dto.getPassword());
      } else {
         return new ServiceResponse(problem);
      }
   }

}
