package io.inprice.scrapper.api.rest.validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.List;

public class EmailDTOValidator {

    public static boolean verify(String email, List<String> problems) {
        if (StringUtils.isBlank(email)) {
            problems.add("Email address cannot be null!");
            return false;
        } else if (email.length() < 9 || email.length() > 250) {
            problems.add("Email address must be between 9 and 250 chars!");
            return false;
        } else if (!EmailValidator.getInstance().isValid(email)) {
            problems.add("Invalid email address!");
            return false;
        }
        return true;
    }

}
