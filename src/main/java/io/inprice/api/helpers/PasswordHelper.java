package io.inprice.api.helpers;

import io.inprice.api.external.Props;
import io.inprice.api.info.Pair;
import jodd.util.BCrypt;

public class PasswordHelper {

  public static Pair<String, String> generateSaltAndHash(String password) {
    String salt = BCrypt.gensalt(Props.APP_SALT_ROUNDS());
    String hash = generateHashOnly(password, salt);
    return new Pair<String, String>(salt, hash);
  }

  public static String generateHashOnly(String password, String salt) {
    return BCrypt.hashpw(password, salt);
  }

}
