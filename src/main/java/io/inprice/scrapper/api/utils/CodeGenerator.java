package io.inprice.scrapper.api.utils;

import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.framework.Beans;
import jodd.util.BCrypt;

public class CodeGenerator {

    private final Config config = Beans.getSingleton(Config.class);

    public String generateSalt() {
        return BCrypt.gensalt(config.getAS_SaltRounds());
    }

}
