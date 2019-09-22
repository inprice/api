package io.inprice.scrapper.api.utils;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import jodd.util.BCrypt;

public class CodeGenerator {

    private static final Properties props = Beans.getSingleton(Properties.class);

    public String generateSalt() {
        return BCrypt.gensalt(props.getAS_SaltRounds());
    }

}
