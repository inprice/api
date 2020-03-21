package io.inprice.scrapper.api.framework;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;

import java.util.Set;

public class ConfigScanner {

   private static final Logger log = LoggerFactory.getLogger("ConfigScanner");

   public static void scan(Javalin app) {
      log.info("- Routers are being scanned...");
      Reflections reflections = new Reflections("io.inprice.scrapper.api.rest", new MethodAnnotationsScanner());
      Set<Class<?>> controllerSet = reflections.getTypesAnnotatedWith(Routing.class);

      for (Class<?> clazz : controllerSet) {
         try {
            log.info(String.format("  + %s : OK", clazz.getSimpleName()));
            ((Controller) clazz.newInstance()).addRoutes(app);
         } catch (Exception e) {
            log.error("Error in scanning Router methods.", e);
         }
      }
   }

}
