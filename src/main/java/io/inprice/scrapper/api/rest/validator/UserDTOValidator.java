package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.ArrayList;
import java.util.List;

public class UserDTOValidator {

    private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

    public static List<Problem> verify(UserDTO userDTO, boolean insert, String field) {
        List<Problem> problems = new ArrayList<>();

        if (StringUtils.isBlank(userDTO.getFullName())) {
            problems.add(new Problem("contactName", field + " name cannot be null!"));
        } else if (userDTO.getFullName().length() < 2 || userDTO.getFullName().length() > 150) {
            problems.add(new Problem("contactName", field + " name must be between 2 and 150 chars!"));
        }

        final String email = userDTO.getEmail();
        if (StringUtils.isBlank(email)) {
            problems.add(new Problem("contactEmail", "Email address cannot be null!"));
        } else if (email.length() < 9 || email.length() > 250) {
            problems.add(new Problem("contactEmail", "Email address must be between 9 and 250 chars!"));
        } else if (!EmailValidator.getInstance().isValid(email)) {
            problems.add(new Problem("contactEmail", "Invalid email address!"));
        } else if (insert && userRepository.findByEmail(email, false).isOK()) {
            problems.add(new Problem("contactEmail", email + " is already used by another user!"));
        }

        if (StringUtils.isBlank(userDTO.getPassword())) {
            problems.add(new Problem("password", "Password cannot be null!"));
        } else if (userDTO.getPassword().length() < 5 || userDTO.getPassword().length() > 16) {
            problems.add(new Problem("password", "Password length must be between 5 and 16 chars!"));
        } else if (!userDTO.getPassword().equals(userDTO.getPasswordAgain())) {
            problems.add(new Problem("password", "Passwords are mismatch!"));
        }

        return problems;
    }

}
