package io.inprice.scrapper.api.dto;

public class LoginDTOValidator {

   public static String verify(LoginDTO dto) {
      String problem = PasswordValidator.verify(dto, false, false);
      if (problem == null) {
         problem = EmailValidator.verify(dto.getEmail());
      }
      return problem;
   }

}
