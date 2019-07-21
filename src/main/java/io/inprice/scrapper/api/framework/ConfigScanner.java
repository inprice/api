package io.inprice.scrapper.api.framework;

import io.inprice.scrapper.api.framework.abs.IController;
import org.reflections.Reflections;

import java.util.Set;

public class ConfigScanner {

    public static void scanForControllers() {
        Reflections reflections = new Reflections("io.inprice.scrapper.api.web");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Controller.class);

        for (Class<?> controller: annotated) {
            try {
                ((IController)controller.newInstance()).addRoutes();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
