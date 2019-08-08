package io.inprice.scrapper.api;

import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.ConfigScanner;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.AuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final Config config = Beans.getSingleton(Config.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public static void main(String[] args) {
        new Thread(() -> {
            log.info("APPLICATION IS STARTING...");

            //spark configs
            port(config.getAPP_Port());

            before(new AuthFilter());
            before((req, res) -> res.type("application/json"));

            ConfigScanner.scan();

            Global.isApplicationRunning = true;

            log.info("APPLICATION STARTED.");
        }, "app-starter").start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("APPLICATION IS TERMINATING...");

            log.info(" - Web server is shutting down...");
            stop();
            awaitStop();

            //log.info(" - TaskManager scheduler is shutting down...");
            //TaskManager.stop();

            //log.info(" - Thread pools are shutting down...");
            //ThreadPools.shutdown();

            //log.info(" - Redis connection is closing...");
            //RedisClient.shutdown();

            //log.info(" - RabbitMQ connection is closing...");
            //RabbitMQ.closeChannel();

            log.info(" - DB connection is closing...");
            dbUtils.shutdown();

            log.info("ALL SERVICES IS DONE.");

            Global.isApplicationRunning = false;
        },"shutdown-hook"));
    }

}
