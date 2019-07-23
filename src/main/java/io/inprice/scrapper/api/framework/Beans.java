package io.inprice.scrapper.api.framework;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class Beans {

    private static Map<Class<?>, Object> singletonMap = new HashMap<>();

    public static <T> T getSingleton(Class<?> clazz) {
        T obj = (T) singletonMap.get(clazz);

        if (obj == null) {
            try {
                Constructor con = clazz.getDeclaredConstructor();
                con.setAccessible(true);
                obj = (T) con.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            singletonMap.put(clazz, obj);
        }
        return obj;
    }

}
