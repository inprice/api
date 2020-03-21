package io.inprice.scrapper.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.ConfigScanner;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Props;
import io.inprice.scrapper.api.helpers.RabbitMQ;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.rest.component.AuthFilter;
import io.inprice.scrapper.api.rest.component.UserInfo;
import io.javalin.Javalin;
import io.javalin.plugin.openapi.annotations.ContentType;

public class Application {

   private static final Logger log = LoggerFactory.getLogger(Application.class);

   private static Javalin app;
   private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

   public static void main(String[] args) {
      new Thread(() -> {
         log.info("APPLICATION IS STARTING...");

         createServer();
         ConfigScanner.scan(app);

         Global.isApplicationRunning = true;

         log.info("APPLICATION STARTED.");
      }, "app-starter").start();

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         log.info("APPLICATION IS TERMINATING...");

         log.info(" - Web server is shutting down...");
         app.stop();

         log.info(" - Redis connection is closing...");
         RedisClient.shutdown();

         log.info(" - RabbitMQ connection is closing...");
         RabbitMQ.closeChannel();

         log.info(" - DB connection is closing...");
         dbUtils.shutdown();

         log.info("ALL SERVICES IS DONE.");

         Global.isApplicationRunning = false;
      }, "shutdown-hook"));
   }

   private static void createServer() {
      app = Javalin.create((config) -> {
         config.defaultContentType = ContentType.JSON;
         config.enableCorsForAllOrigins();
         config.enforceSsl = !Props.isRunningForTests();
         config.logIfServerNotStarted = true;
         config.showJavalinBanner = true;
      }).start(Props.getAPP_Port());

      app.before(new AuthFilter());
      app.after(ctx -> UserInfo.cleanup());
   }

}
