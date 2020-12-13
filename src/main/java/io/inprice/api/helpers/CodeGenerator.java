package io.inprice.api.helpers;

import java.util.UUID;

public class CodeGenerator {
  
  public static String hash() {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
  }

}
