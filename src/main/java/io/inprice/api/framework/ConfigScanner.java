package io.inprice.api.framework;

import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;

public class ConfigScanner {

   private static final Logger logger = LoggerFactory.getLogger("ConfigScanner");

   public static void scanControllers(Javalin app) {
      logger.info("- Routers are being scanned...");
      Reflections reflections = new Reflections("io.inprice.api.app", new SubTypesScanner(), new TypeAnnotationsScanner());
      Set<Class<?>> controllerSet = reflections.getTypesAnnotatedWith(Router.class);

      for (Class<?> clazz : controllerSet) {
         try {
            logger.info(String.format("  + %s : OK", clazz.getSimpleName()));
            ((AbstractController) clazz.getDeclaredConstructor().newInstance()).addRoutes(app);
         } catch (Exception e) {
            logger.error("Error in scanning Router methods.", e);
         }
      }
   }

}
