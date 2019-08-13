package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.info.Problem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.List;

public class EmailDTOValidator {

    public static boolean verify(String email, List<Problem> problems) {
        if (StringUtils.isBlank(email)) {
            problems.add(new Problem("email", "Email address cannot be null!"));
            return false;
        } else if (email.length() < 9 || email.length() > 250) {
            problems.add(new Problem("email", "Email address must be between 9 and 250 chars!"));
            return false;
        } else if (!EmailValidator.getInstance().isValid(email)) {
            problems.add(new Problem("email", "Invalid email address!"));
            return false;
        }
        return true;
    }

}
