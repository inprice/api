package io.inprice.scrapper.api.dto;

import org.apache.commons.lang3.StringUtils;

public class NameAndEmailValidator {

   public static String verify(String name, String email) {
      if (StringUtils.isBlank(name)) {
         return "Name cannot be null!";
      } else if (name.length() < 2 || name.length() > 70) {
         return "Name must be between 2 and 70 chars!";
      }

      return EmailValidator.verify(email);
   }

}
