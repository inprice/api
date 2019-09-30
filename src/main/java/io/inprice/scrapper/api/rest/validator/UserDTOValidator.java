package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.User;
import org.apache.commons.lang3.StringUtils;

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

        //when it is being created, no need to check for passwords, since they are updated and checked in different service calls
        if (insert) {
            problems.addAll(PasswordDTOValidator.verify(userDTO, true, false));
        }

        boolean verifiedByEmailDTOValidator = EmailDTOValidator.verify(userDTO.getEmail(), problems);

        if (verifiedByEmailDTOValidator) {
            ServiceResponse<User> found = null;
            if (insert) {
                found = userRepository.findByEmail(userDTO.getEmail());
            } else if (userDTO.getId() != null) {
                found = userRepository.findByEmailForUpdateCheck(userDTO.getEmail(), userDTO.getId());
            }
            if (found != null && found.isOK()) {
                problems.add(new Problem("email", userDTO.getEmail() + " is already used by another user!"));
            }
        }

        return problems;
    }

}
