package io.inprice.scrapper.api;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.crawler.common.meta.UserType;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.models.Customer;
import io.inprice.scrapper.api.service.CustomerService;
import org.eclipse.jetty.http.HttpStatus;
import spark.Filter;
import spark.Spark;

import static spark.Spark.*;

public class Application {

    private static final Logger log = new Logger(Application.class);

    public static void main(String[] args) {
        log.info("Scrapper API is starting...");

        port(8080);
        addCORSFilters();

        get("/healthy", (req, res) -> "OK");
        get("/ready", (req, res) -> Global.isRunning);

        post("/signup", (req, res) -> {
            Customer customer = CustomerService.get().signup(req.body(), UserType.USER);
            String result = Global.gson.toJson(customer);

            if (customer.getHttpStatus() != HttpStatus.OK_200) {
                halt(customer.getHttpStatus(), result);
            }

            return result;
        });

        post("/logout", (req, res) -> {
            String apiKey = req.headers(Consts.API_KEY);
            if (apiKey != null) {
                RedisClient.removeApiKey(apiKey);
            }

            String token = req.headers(Consts.CSRF_TOKEN);
            if (token != null) {
                RedisClient.removeToken(token);
            }

            return "OK";
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Global.isRunning = false;

            log.info("Redis connection is closing...");
            RedisClient.shutdown();

            log.info("DB connection is closing...");
            DBUtils.close();

            log.info("Connections are closed.");
        }));

        Global.isRunning = true;

        log.info("Scrapper API is started on %d", Spark.port());
    }

    private static void addCORSFilters() {
        Filter filter = (request, response) -> {
            //TODO: token denetimleri eklenecek
            response.header("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Content-Length, Accept,Origin");
            response.header("Access-Control-Allow-Credentials", "true");
        };
        after(filter);
    }

}
