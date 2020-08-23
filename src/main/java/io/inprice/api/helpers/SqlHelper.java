package io.inprice.api.helpers;

import org.apache.commons.lang3.StringUtils;

public class SqlHelper {

  // http://www.java2s.com/Code/Java/Database-SQL-JDBC/EscapeSQL.htm
  public static String clear(String val) {
    if (StringUtils.isBlank(val))
      return val;

    int length = val.length();
    int newLength = length;
    // first check for characters that might
    // be dangerous and calculate a length
    // of the string that has escapes.
    for (int i = 0; i < length; i++) {
      char c = val.charAt(i);
      switch (c) {
        case '\\':
        case '\"':
        case '\'':
        case '\0': {
          newLength += 1;
        }
          break;
      }
    }
    if (length == newLength) {
      // nothing to escape in the string
      return val;
    }
    StringBuffer sb = new StringBuffer(newLength);
    for (int i = 0; i < length; i++) {
      char c = val.charAt(i);
      switch (c) {
        case '\\': {
          sb.append("\\\\");
        }
          break;
        case '\"': {
          sb.append("\\\"");
        }
          break;
        case '\'': {
          sb.append("\\\'");
        }
          break;
        case '\0': {
          sb.append("\\0");
        }
          break;
        default: {
          sb.append(c);
        }
      }
    }
    return sb.toString().trim();
  }

}
