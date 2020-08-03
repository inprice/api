package io.inprice.api.helpers;

import java.util.Date;

import io.inprice.api.external.Props;
import jodd.util.BCrypt;

public class CodeGenerator {

  public String generateSalt() {
    return BCrypt.gensalt(Props.APP_SALT_ROUNDS());
  }

  public static void main(String[] args) {
    Date now = new Date();
    long millis = now.getTime() - (now.getTime() / 1000);

    Date found = new Date((1596438662 * 1000) + millis);

    System.out.println(found);
  }

}
