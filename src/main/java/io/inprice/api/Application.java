package io.inprice.api;

import com.stripe.Stripe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Global;
import io.inprice.api.consumer.ProductCreationFromLinkConsumer;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.framework.ConfigScanner;
import io.inprice.api.framework.HandlerInterruptException;
import io.inprice.api.helpers.ThreadPools;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.AccessGuard;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
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

      new ProductCreationFromLinkConsumer().start();      

      log.info("APPLICATION STARTED.");
      Global.isApplicationRunning = true;

      Stripe.apiKey = Props.API_KEYS_STRIPE();

    }, "app-starter").start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {

      Global.isApplicationRunning = false;
      log.info("APPLICATION IS TERMINATING...");

			log.info(" - Thread pools are shutting down...");
			ThreadPools.shutdown();

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
      config.requestCacheSize = 8192L;

      if (SysProps.APP_ENV().equals(AppEnv.DEV)) {
        config.registerPlugin(new RouteOverviewPlugin("/routes"));
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
