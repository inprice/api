package io.inprice.api.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class EmailValidator {

  private final static Pattern pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

  public static String verify(String email) {
    final Matcher matcher = pattern.matcher(email);
    if (StringUtils.isBlank(email)) {
      return "Email address cannot be empty!";
    } else if (email.length() < 9 || email.length() > 100) {
      return "Email address must be between 9 and 100 chars!";
    } else if (!matcher.matches()) {
      return "Invalid email address!";
    }
    return null;
  }

}
