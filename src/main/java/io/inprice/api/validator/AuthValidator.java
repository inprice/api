package io.inprice.api.validator;

import io.inprice.api.app.user.dto.LoginDTO;

public class AuthValidator {

  public static String verify(LoginDTO dto) {
    String problem = PasswordValidator.verify(dto, false, false);
    if (problem == null) {
      problem = EmailValidator.verify(dto.getEmail());
    }
    return problem;
  }

}
