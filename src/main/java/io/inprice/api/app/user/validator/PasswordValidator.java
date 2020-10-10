package io.inprice.api.app.user.validator;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.User;

public class PasswordValidator {

  public static String verify(PasswordDTO dto, boolean repeatPassCheck, boolean oldPassCheck) {
    String problem = null;

    if (StringUtils.isBlank(dto.getPassword())) {
      problem = "Password cannot be null!";
    } else if (dto.getPassword().length() < 4 || dto.getPassword().length() > 16) {
      problem = "Password length must be between 4 and 16 chars!";
    } else if (repeatPassCheck && !dto.getPassword().equals(dto.getRepeatPassword())) {
      problem = "Passwords are mismatch!";
    }

    if (problem == null && oldPassCheck) {
      if (StringUtils.isBlank(dto.getOldPassword())) {
        problem = "Old password cannot be null!";
      } else {
        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);
          User user = userDao.findById(dto.getId());
          if (user != null) {
            final String hash = PasswordHelper.generateHashOnly(dto.getOldPassword(), user.getPasswordSalt());
            if (! hash.equals(user.getPasswordHash())) {
              problem = "Old password is incorrect!";
            }
          } else {
            problem = Responses.NotFound.USER.getReason();
          }
        }
      }
    }

    return problem;
  }

}