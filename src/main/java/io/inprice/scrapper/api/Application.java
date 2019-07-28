package io.inprice.scrapper.api;

import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.ConfigScanner;
import io.inprice.scrapper.api.helpers.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import static spark.Spark.*;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final Config config = Beans.getSingleton(Config.class);

    public static void main(String[] args) {
        new Thread(() -> {
            log.info("APPLICATION IS STARTING...");

            //spark configs
            port(config.getAPP_Port());
            before((req, res) -> res.type("application/json"));

            ConfigScanner.scan();

            Global.isApplicationRunning = true;

            log.info("APPLICATION STARTED.");
        }, "app-starter").start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("APPLICATION IS TERMINATING...");
            Global.isApplicationRunning = false;

            log.info(" - Web server is shutting down...");
            Spark.stop();

            //log.info(" - TaskManager scheduler is shutting down...");
            //TaskManager.stop();

            //log.info(" - Thread pools are shutting down...");
            //ThreadPools.shutdown();

            //log.info(" - Redis connection is closing...");
            //RedisClient.shutdown();

            //log.info(" - RabbitMQ connection is closing...");
            //RabbitMQ.closeChannel();

            log.info(" - DB connection is closing...");
            //DBUtils.shutdown();

            shutdown();

            log.info("ALL SERVICES IS DONE.");
        },"shutdown-hook"));
    }

    public static void shutdown() {
        stop();
    }

}
