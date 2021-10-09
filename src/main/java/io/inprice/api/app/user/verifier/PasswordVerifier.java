package io.inprice.api.app.user.verifier;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.app.user.dto.PasswordDTO;

public class PasswordVerifier {

  public static String verify(PasswordDTO dto) {
  	return verify(dto, true);
  }

  public static String verify(PasswordDTO dto, boolean repeatPassCheck) {
    String problem = null;

    if (StringUtils.isBlank(dto.getPassword())) {
      problem = "Password cannot be empty!";
    } else if (dto.getPassword().length() < 6 || dto.getPassword().length() > 16) {
      problem = "Password length must be between 6 - 16 chars!";
    } else if (repeatPassCheck && !dto.getPassword().equals(dto.getRepeatPassword())) {
      problem = "Passwords are mismatch!";
    }

    return problem;
  }

}
