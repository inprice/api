package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.ArrayList;
import java.util.List;

public class UserDTOValidator {

    private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

    public static List<Problem> verify(UserDTO userDTO, boolean insert, String field) {
        List<Problem> problems = new ArrayList<>();

        if (! insert && userDTO.getId() == null) {
            problems.add(new Problem("fullName", "User id cannot be null!"));
        }

        if (userDTO.getType() != null && userDTO.getType().equals(UserType.ADMIN)) {
            problems.add(new Problem("fullName", "Admin user cannot be edited in this way!"));
        }

        if (StringUtils.isBlank(userDTO.getFullName())) {
            problems.add(new Problem("fullName", field + " name cannot be null!"));
        } else if (userDTO.getFullName().length() < 2 || userDTO.getFullName().length() > 150) {
            problems.add(new Problem("fullName", field + " name must be between 2 and 150 chars!"));
        }

        //when it is updated, no need to check for passwords since they are updated and checked in different service calls
        if (insert) problems.addAll(PasswordDTOValidator.verify(userDTO, false));

        final String email = userDTO.getEmail();
        if (StringUtils.isBlank(email)) {
            problems.add(new Problem("email", "Email address cannot be null!"));
        } else if (email.length() < 4 || email.length() > 250) {
            problems.add(new Problem("email", "Email address must be between 4 and 250 chars!"));
        } else if (!EmailValidator.getInstance().isValid(email)) {
            problems.add(new Problem("email", "Invalid email address!"));
        } else {
            ServiceResponse<User> found = null;
            if (insert) {
                found = userRepository.findByEmail(email);
            } else if (userDTO.getId() != null) {
                found = userRepository.findByEmailForUpdateCheck(email, userDTO.getId());
            }
            if (found != null && found.isOK()) {
                problems.add(new Problem("email", email + " is already used by another user!"));
            }
        }

        return problems;
    }

}
