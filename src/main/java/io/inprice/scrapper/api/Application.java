package io.inprice.scrapper.api;

import static spark.Spark.awaitStop;
import static spark.Spark.stop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.ConfigScanner;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.RabbitMQ;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.rest.component.AuthFilter;
import io.inprice.scrapper.api.rest.component.Context;
import spark.Spark;

public class Application {

   private static final Logger log = LoggerFactory.getLogger(Application.class);

   private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

   public static void main(String[] args) {
      new Thread(() -> {
         log.info("APPLICATION IS STARTING...");

         applySparkSettings();
         ConfigScanner.scan();

         Global.isApplicationRunning = true;

         log.info("APPLICATION STARTED.");
      }, "app-starter").start();

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         log.info("APPLICATION IS TERMINATING...");

         log.info(" - Web server is shutting down...");
         stop();
         awaitStop();

         // log.info(" - TaskManager scheduler is shutting down...");
         // TaskManager.stop();

         // log.info(" - Thread pools are shutting down...");
         // ThreadPools.shutdown();

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

   private static void applySparkSettings() {
      Spark.port(Props.getAPP_Port());

      Spark.before(new AuthFilter());

      // Enable CORS
      Spark.options("/*", (req, res) -> {
         String acrh = req.headers("Access-Control-Request-Headers");
         if (acrh != null)
            res.header("Access-Control-Allow-Headers", acrh);

         String acrm = req.headers("Access-Control-Request-Method");
         if (acrm != null)
            res.header("Access-Control-Allow-Methods", acrm);

         return "OK";
      });

      Spark.before((req, res) -> {
         res.header("Access-Control-Allow-Origin", "*");
         res.header("Access-Control-Request-Methods", "*");
         res.header("Access-Control-Expose-Headers", "Authorization, workspace");
         if (req.requestMethod().equals("OPTIONS")) {
            res.header("Access-Control-Max-Age", "86400");
         }
         res.type("application/json");
      });

      Spark.after((request, response) -> Context.cleanup());

      /*
       * if (Props.isRunningForTests()) { Spark.after((request, response)-> { if
       * (response.status() != HttpStatus.OK_200) { StringBuilder sb = new
       * StringBuilder("Request  -> "); sb.append(request.requestMethod());
       * sb.append(" - "); sb.append(request.url()); if (request.body().length() > 0)
       * { sb.append(" -> "); sb.append(request.body()); } log.warn(sb.toString());
       * 
       * sb.setLength(0); sb.append("Response -> "); sb.append(response.body());
       * log.warn(sb.toString()); } }); }
       */
   }

}
