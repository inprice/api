package io.inprice.scrapper.api.utils;

import io.inprice.scrapper.api.helpers.Props;
import jodd.util.BCrypt;

public class CodeGenerator {

   public String generateSalt() {
      return BCrypt.gensalt(Props.getAS_SaltRounds());
   }

}
