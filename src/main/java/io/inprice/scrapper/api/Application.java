package io.inprice.scrapper.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Global;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.ConfigScanner;
import io.inprice.scrapper.api.framework.HandlerInterruptException;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.AccessGuard;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.meta.AppEnv;
import io.inprice.scrapper.common.config.SysProps;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.javalin.Javalin;
import io.javalin.core.util.Header;
import io.javalin.core.util.RouteOverviewPlugin;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.openapi.annotations.ContentType;

public class Application {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static Javalin app;
  private static final Database db = Beans.getSingleton(Database.class);

  public static void main(String[] args) {
    new Thread(() -> {
      log.info("APPLICATION IS STARTING...");

      createServer();
      ConfigScanner.scanControllers(app);

      log.info("APPLICATION STARTED.");
      Global.isApplicationRunning = true;

    }, "app-starter").start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {

      Global.isApplicationRunning = false;
      log.info("APPLICATION IS TERMINATING...");

      log.info(" - Web server is shutting down...");
      app.stop();

      log.info(" - Redis connection is closing...");
      RedisClient.shutdown();

      log.info(" - RabbitMQ connection is closing...");
      RabbitMQ.closeConnection();

      log.info(" - DB connection is closing...");
      db.shutdown();

      log.info("ALL SERVICES IS DONE.");

    }, "shutdown-hook"));
  }

  private static void createServer() {
    app = Javalin.create((config) -> {
      config.defaultContentType = ContentType.JSON;
      config.enableCorsForAllOrigins();
      config.logIfServerNotStarted = true;
      config.showJavalinBanner = false;

      if (SysProps.APP_ENV().equals(AppEnv.DEV)) {
        config.registerPlugin(new RouteOverviewPlugin("/routes"));
      } else {
        config.enforceSsl = true;
      }

      config.accessManager(new AccessGuard());

      JavalinJackson.configure(JsonConverter.mapper);
    }).start(Props.APP_PORT());

    app.before(ctx -> {
      if (ctx.method() == "OPTIONS") {
        ctx.header(Header.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      }
    });

    app.after(ctx -> CurrentUser.cleanup());

    app.exception(HandlerInterruptException.class, (e, ctx) -> {
      ctx.json(new ServiceResponse(e.getStatus(), e.getMessage()));
    });
  }

}
