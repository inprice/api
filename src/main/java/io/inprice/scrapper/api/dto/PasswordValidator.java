package io.inprice.scrapper.api.dto;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import jodd.util.BCrypt;

public class PasswordValidator {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

   public static String verify(PasswordDTO dto, boolean repeatPassCheck, boolean oldPassCheck) {
      if (StringUtils.isBlank(dto.getPassword())) {
         return "Password cannot be null!";
      } else if (dto.getPassword().length() < 4 || dto.getPassword().length() > 16) {
         return "Password length must be between 4 and 16 chars!";
      } else if (repeatPassCheck && !dto.getPassword().equals(dto.getRepeatPassword())) {
         return "Passwords are mismatch!";
      }

      if (oldPassCheck) {
         if (StringUtils.isBlank(dto.getOldPassword())) {
            return "Old password cannot be null!";
         } else {
            ServiceResponse found = userRepository.findById(dto.getId(), true);
            User user = found.getData();
            if (found.isOK()) {
               final String hash = BCrypt.hashpw(dto.getOldPassword(), user.getPasswordSalt());
               if (! hash.equals(user.getPasswordHash())) {
                  return "Old password is incorrect!";
               }
            }
         }
      }

      return null;
   }

}
