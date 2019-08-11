package io.inprice.scrapper.api.utils;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import jodd.util.BCrypt;

public class CodeGenerator {

    private final Properties properties = Beans.getSingleton(Properties.class);

    public String generateSalt() {
        return BCrypt.gensalt(properties.getAS_SaltRounds());
    }

}
