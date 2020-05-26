package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.external.Props;
import jodd.util.BCrypt;

public class CodeGenerator {

   public String generateSalt() {
      return BCrypt.gensalt(Props.APP_SALT_ROUNDS());
   }

}
