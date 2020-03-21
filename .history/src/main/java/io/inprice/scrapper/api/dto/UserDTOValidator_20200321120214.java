package io.inprice.scrapper.api.dto;

import org.apache.commons.lang3.StringUtils;

public class UserDTOValidator {

   public static String verify(UserDTO dto) {
      if (StringUtils.isBlank(dto.getName())) {
         return "Name cannot be null!";
      } else if (dto.getName().length() < 2 || dto.getName().length() > 70) {
         return "Name must be between 2 and 70 chars!";
      }

      return EmailValidator.verify(dto.getEmail());
   }

}
