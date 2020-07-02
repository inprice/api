package io.inprice.api.helpers;

import io.inprice.api.external.Props;
import jodd.util.BCrypt;

public class CodeGenerator {

   public String generateSalt() {
      return BCrypt.gensalt(Props.APP_SALT_ROUNDS());
   }

}
