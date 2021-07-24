package io.inprice.api.app.user.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class EmailValidator {

  private static final Pattern pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

  public static String verify(String email) {
  	if (StringUtils.isBlank(email)) return "Email address cannot be empty!";
  	
  	final Matcher matcher = pattern.matcher(email);
    if (email.length() < 8 || email.length() > 128) {
      return "Email address must be between 8 - 128 chars!";
    } else if (!matcher.matches()) {
      return "Invalid email address!";
    }
    return null;
  }

}
