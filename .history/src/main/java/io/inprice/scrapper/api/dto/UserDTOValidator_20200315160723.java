package io.inprice.scrapper.api.app.user;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.validator.EmailDTOValidator;

public class UserDTOValidator {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

   public static List<String> verify(UserDTO userDTO, boolean insert) {
      List<String> problems = new ArrayList<>();

      if (!insert && userDTO.getId() == null) {
         problems.add("User id cannot be null!");
      }

      if (userDTO.getRole() == null) {
         problems.add("User role cannot be null! Please specify either EDITOR or READER.");
      }

      if (userDTO.getStatus() == null) {
         problems.add("User status cannot be null! Please specify either ACTIVE or PASSIVE.");
      }

      if (userDTO.getRole() != null && userDTO.getRole().equals(UserRole.ADMIN)) {
         problems.add("Admin user cannot be edited in this way! Please use settings section instead.");
      }

      if (StringUtils.isBlank(userDTO.getFullName())) {
         problems.add(field + " name cannot be null!");
      } else if (userDTO.getFullName().length() < 2 || userDTO.getFullName().length() > 150) {
         problems.add(field + " name must be between 2 and 150 chars!");
      }

      boolean verifiedByEmailDTOValidator = EmailDTOValidator.verify(userDTO.getEmail(), problems);

      if (verifiedByEmailDTOValidator) {
         ServiceResponse found = null;
         if (insert) {
            found = userRepository.findByEmailForInsertCheck(userDTO.getEmail());
         } else if (userDTO.getId() != null) {
            found = userRepository.findByEmailForUpdateCheck(userDTO.getEmail(), userDTO.getId());
         }
         if (found != null && found.isOK()) {
            problems.add(
                  "A user with " + userDTO.getEmail() + " address is already defined! Status: " + found.getStatus());
         }
      }

      return problems;
   }

}
