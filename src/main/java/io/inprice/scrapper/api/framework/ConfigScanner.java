package io.inprice.scrapper.api.framework;

import io.inprice.scrapper.common.logging.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.reflect.Method;
import java.util.Set;

public class ConfigScanner {

    private static final Logger log = new Logger("ConfigScanner");

    public static void scan() {
        log.info("- Routes are adding...");
        scanRoutings();
    }

    private static void scanRoutings() {
        Reflections reflections = new Reflections("io.inprice.scrapper.api.rest", new MethodAnnotationsScanner());
        Set<Method> routingMethodSet = reflections.getMethodsAnnotatedWith(Routing.class);

        for (Method method: routingMethodSet) {
            try {
                Class<?> declaringClass = method.getDeclaringClass();

                log.info(String.format("   + %s : OK", declaringClass.getSimpleName()));

                method.invoke(declaringClass.newInstance());
            } catch (Exception e) {
                log.error("Error in scanning Router methods.", e);
            }
        }
    }

}
