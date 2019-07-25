package io.inprice.scrapper.api.framework;

import io.inprice.scrapper.common.logging.Logger;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class Beans {

    private static final Logger log = new Logger(Beans.class);

    private static Map<Class<?>, Object> singletonMap = new HashMap<>();

    public synchronized static <T> T getSingleton(Class<?> clazz) {
        T obj = (T) singletonMap.get(clazz);

        if (obj == null) {
            try {
                Constructor con = clazz.getDeclaredConstructor();
                con.setAccessible(true);
                obj = (T) con.newInstance();
            } catch (Exception e) {
                log.error(e);
            }
            singletonMap.put(clazz, obj);
        }
        return obj;
    }

}
