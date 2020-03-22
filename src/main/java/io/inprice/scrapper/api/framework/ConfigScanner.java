package io.inprice.scrapper.api.framework;

import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;

public class ConfigScanner {

   private static final Logger log = LoggerFactory.getLogger("ConfigScanner");

   public static void scanControllers(Javalin app) {
      log.info("- Routers are being scanned...");
      Reflections reflections = new Reflections("io.inprice.scrapper.api.app", new SubTypesScanner(), new TypeAnnotationsScanner());
      Set<Class<?>> controllerSet = reflections.getTypesAnnotatedWith(Router.class);

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
