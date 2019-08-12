package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.info.Problem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.List;

public class LoginDTOValidator {

    public static List<Problem> verify(LoginDTO loginDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(null, loginDTO, false, false);

        final String email = loginDTO.getEmail();
        if (StringUtils.isBlank(email)) {
            problems.add(new Problem("email", "Email address cannot be null!"));
        } else if (email.length() < 9 || email.length() > 250) {
            problems.add(new Problem("email", "Email address must be between 9 and 250 chars!"));
        } else if (!EmailValidator.getInstance().isValid(email)) {
            problems.add(new Problem("email", "Invalid email address!"));
        }

        return problems;
    }

}
