package io.inprice.scrapper.api;

import io.inprice.scrapper.api.framework.ConfigScanner;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.common.logging.Logger;
import spark.Spark;

public class Application {

    private static final Logger log = new Logger(Application.class);

    public static void main(String[] args) {
        new Thread(() -> {
            Global.isApplicationRunning = true;

            ConfigScanner.scanForControllers();

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
            DBUtils.shutdown();

            log.info("ALL SERVICES IS DONE.");
        },"shutdown-hook"));
    }

}
