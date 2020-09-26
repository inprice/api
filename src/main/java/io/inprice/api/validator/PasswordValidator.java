package io.inprice.api.validator;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.user.UserRepository;
import io.inprice.api.info.ServiceResponse;
import io.inprice.common.helpers.Beans;
import io.inprice.common.models.User;
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
        ServiceResponse found = userRepository.findById(dto.getId());
        if (found.isOK()) {
          User user = found.getData();
          final String hash = BCrypt.hashpw(dto.getOldPassword(), user.getPasswordSalt());
          if (!hash.equals(user.getPasswordHash())) {
            return "Old password is incorrect!";
          }
        } else {
          return found.getReason();
        }
      }
    }

    return null;
  }

}
